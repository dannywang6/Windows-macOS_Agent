package com.studyagent.main;

import com.studyagent.analyze.CategoryAnalyzer;
import com.studyagent.localserver.TabServer;
import com.studyagent.model.ActiveTab;
import com.studyagent.model.ActivityRecord;
import com.studyagent.monitor.IdleMonitor;
import com.studyagent.monitor.ProcessMonitor;
import com.studyagent.storage.SQLiteManager;
import com.studyagent.upload.ApiClient;

import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class Application {

    private static final long INTERVAL_MILLIS = 5000;
    private static final long IDLE_THRESHOLD_MILLIS = 60_000;

    private final ProcessMonitor monitor;
    private final IdleMonitor idleMonitor;
    private final CategoryAnalyzer analyzer;
    private final SQLiteManager db;
    private final ApiClient api;
    private final TabServer tabServer;

    private String currentApp;
    private String currentTitle;
    private String currentUrl;
    private LocalDateTime currentStart;
    private long idleAccumulatedMillis;

    public Application(String dbPath, String serverUrl) {
        this.analyzer = new CategoryAnalyzer();
        this.monitor = ProcessMonitor.create();
        this.idleMonitor = IdleMonitor.create();
        this.db = new SQLiteManager(dbPath);
        this.api = new ApiClient(serverUrl);
        this.tabServer = new TabServer();
    }

    public void start() throws SQLException, InterruptedException {
        db.init();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            tabServer.start(9876);
        } catch (IOException e) {
            System.out.println("TabServer 启动失败: " + e.getMessage());
        }

        while (true) {
            String app = monitor.getActiveProcessName();
            String title = monitor.getActiveWindowTitle();

            // 浏览器时，扩展会上报当前激活标签页，用它判断是否切了网页
            String latestUrl = null;
            String latestTitle = null;
            if (analyzer.isBrowser(app)) {
                ActiveTab tab = tabServer.getLatestTab();
                if (tab != null) {
                    latestUrl = tab.getUrl();
                    latestTitle = tab.getTitle();
                }
            }

            // 触发"切换"的两种情形：
            // 1) 前台进程变了（换了应用）
            // 2) 仍在浏览器里，但激活标签页的 URL 变了（bilibili -> github）
            boolean appChanged = app != null && !app.equals(currentApp);
            boolean urlChanged = !appChanged
                    && currentUrl != null
                    && latestUrl != null
                    && !latestUrl.equals(currentUrl);

            if (appChanged || urlChanged) {
                String newTitle = title;
                String newUrl = null;
                if (analyzer.isBrowser(app)) {
                    newUrl = latestUrl;
                    if (latestTitle != null && !latestTitle.isBlank()) {
                        newTitle = latestTitle;
                    }
                }
                beginActivity(app, newTitle, newUrl);
                System.out.println(appChanged
                        ? "Switched to: " + app
                        : "Tab changed to: " + newUrl);
            }

            if (idleMonitor.getLastInputIdleMillis() >= IDLE_THRESHOLD_MILLIS) {
                idleAccumulatedMillis += INTERVAL_MILLIS;
            }

            uploadPendingRecords();
            cleanupOldRecords();
            Thread.sleep(INTERVAL_MILLIS);
        }
    }

    private void beginActivity(String app, String title, String url) {
        finishCurrentActivity();
        currentApp = app;
        currentTitle = title;
        currentUrl = url;
        idleAccumulatedMillis = 0;
        currentStart = LocalDateTime.now();
    }

    private void finishCurrentActivity() {
        if (currentApp == null) {
            return;
        }
        long effectiveDuration = Math.max(0,
                Duration.between(currentStart, LocalDateTime.now()).toMillis() - idleAccumulatedMillis);
        ActivityRecord record = new ActivityRecord(
                0L,
                currentApp,
                currentTitle,
                currentUrl,
                analyzer.analyze(currentApp, currentTitle, currentUrl),
                currentStart,
                effectiveDuration,
                false);

        try {
            db.save(record);
            System.out.println("Saved: " + record.getApp()
                    + "  " + record.getDurationMillis() + " ms  [" + record.getCategory() + "]");
        } catch (SQLException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private void uploadPendingRecords() {
        try {
            List<ActivityRecord> pending = db.findUnuploaded();
            if (!pending.isEmpty()) {
                api.report(pending);
            }
        } catch (SQLException e) {
            System.out.println("Upload check failed: " + e.getMessage());
        }
    }

    private void cleanupOldRecords() {
        try {
            int deleted = db.deleteUploadedBefore(LocalDateTime.now().minusDays(3));
            if (deleted > 0) {
                System.out.println("Cleaned " + deleted + " old records");
            }
        } catch (SQLException e) {
            System.out.println("Cleanup failed: " + e.getMessage());
        }
    }

    private void shutdown() {
        finishCurrentActivity();
        try {
            db.close();
        } catch (SQLException e) {
            System.out.println("Close failed: " + e.getMessage());
        }
    }

    public static void main(String[] args) throws Exception {
        new Application("activity.db", "http://localhost:8080").start();
    }
}