package com.studyagent.main;

import com.studyagent.analyze.CategoryAnalyzer;
import com.studyagent.localserver.TabServer;
import com.studyagent.model.ActiveTab;
import com.studyagent.model.ActivityRecord;
import com.studyagent.model.IdleRecord;
import com.studyagent.monitor.IdleMonitor;
import com.studyagent.monitor.ProcessMonitor;
import com.studyagent.storage.SQLiteManager;
import com.studyagent.upload.ApiClient;
import com.studyagent.config.Config;
import com.studyagent.config.ConfigLoader;
import com.studyagent.util.LogBuffer;
import com.studyagent.webui.WebUiServer;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Application {

    private final long intervalMillis;
    private final long idleThresholdMillis;
    private final int tabServerPort;
    private final int guiPort;

    private final ProcessMonitor monitor;
    private final IdleMonitor idleMonitor;
    private final CategoryAnalyzer analyzer;
    private final SQLiteManager db;
    private final ApiClient api;
    private final TabServer tabServer;
    private final WebUiServer webServer;
    private final LogBuffer logBuffer;

    private String currentApp;
    private String currentTitle;
    private String currentUrl;
    private LocalDateTime currentStart;
    private boolean isIdle;
    private LocalDateTime idleStartTime;

    public Application(Config config) {
        this.analyzer = new CategoryAnalyzer(config);
        this.monitor = ProcessMonitor.create();
        this.idleMonitor = IdleMonitor.create();
        this.intervalMillis = config.getIntervalMillis();
        this.idleThresholdMillis = config.getIdleThresholdMillis();
        this.tabServerPort = config.getTabServerPort();
        this.guiPort = config.getGuiPort();
        this.db = new SQLiteManager(config.getDbPath());
        this.api = new ApiClient(config.getServer().getBaseUrl(), config.getServer().getReportPath());
        this.tabServer = new TabServer();
        this.logBuffer = new LogBuffer(500);
        this.logBuffer.install();
        this.webServer = new WebUiServer(db, logBuffer, this::uploadPendingRecords);
    }

    public void start() throws SQLException, InterruptedException {
        db.init();
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            tabServer.start(tabServerPort);
        } catch (IOException e) {
            System.out.println("TabServer 启动失败: " + e.getMessage());
        }

        try {
            webServer.start(guiPort);
        } catch (IOException e) {
            System.out.println("Web UI 启动失败: " + e.getMessage());
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


            // Idle 状态机：检测用户是否离开电脑
            if (idleMonitor.getLastInputIdleMillis() >= idleThresholdMillis) {
                if (!isIdle) {
                    // 刚进入 Idle，记录开始时间
                    isIdle = true;
                    idleStartTime = LocalDateTime.now();
                    System.out.println("User idle detected");
                }
            } else {
                if (isIdle) {
                    // 用户回来了，结束这段 Idle 并保存
                    endIdle();
                    isIdle = false;
                    idleStartTime = null;
                    System.out.println("User returned from idle");
                }
            }

            uploadPendingRecords();
            cleanupOldRecords();
            Thread.sleep(intervalMillis);
        }
    }

    private void beginActivity(String app, String title, String url) {
        finishCurrentActivity();
        currentApp = app;
        currentTitle = title;
        currentUrl = url;
        currentStart = LocalDateTime.now();
    }

    private void finishCurrentActivity() {
        if (currentApp == null) {
            return;
        }
        // 如果还在 Idle 状态，先结束它
        if (isIdle && idleStartTime != null) {
            endIdle();
        }

        ActivityRecord record = new ActivityRecord(
                0L,
                currentApp,
                currentTitle,
                currentUrl,
                analyzer.analyze(currentApp, currentTitle, currentUrl),
                null,
                currentStart,
                LocalDateTime.now(),
                false);

        try {
            db.save(record);
            System.out.println("Saved: " + record.getApp() +
                    "  [" + record.getCategory() + "]");
        } catch (SQLException e) {
            System.out.println("Save failed: " + e.getMessage());
        }
    }

    private void endIdle() {
        if (idleStartTime == null) {
            return;
        }
        // 找到当前活动的数据库 ID（刚存进去的那条）
        try {
            List<ActivityRecord> records = db.findRecords(1, 0);
            if (!records.isEmpty()) {
                long activityId = records.get(0).getId();
                IdleRecord idle = new IdleRecord(0L, activityId, idleStartTime, LocalDateTime.now(), false);
                db.saveIdle(idle);
                System.out.println("Saved idle record: " + activityId);
            }
        } catch (SQLException e) {
            System.out.println("Save idle failed: " + e.getMessage());
        }
    }

    private void uploadPendingRecords() {
        try {
            List<ActivityRecord> pending = db.findUnuploaded();
            if (pending.isEmpty()) {
                return;
            }
            // 给每条 activity 填充关联的 idle 记录
            List<Long> ids = pending.stream().map(ActivityRecord::getId).toList();
            var idleMap = db.findIdleByActivityIds(ids);
            for (ActivityRecord record : pending) {
                List<IdleRecord> idles = idleMap.getOrDefault(record.getId(), new ArrayList<>());
                record.setIdleRecords(idles);
            }
            if (api.report(pending)) {
                for (ActivityRecord record : pending) {
                    db.markUploaded(record.getId());
                    for (IdleRecord idle : record.getIdleRecords()) {
                        db.markIdleUploaded(idle.getId());
                    }
                }
                System.out.println("Uploaded " + pending.size() + " records (with idle)");
            }
        } catch (SQLException e) {
            System.out.println("Upload check failed: " + e.getMessage());
        }
    }

    private void cleanupOldRecords() {
        try {
            int deleted = db.deleteUploadedBefore(LocalDateTime.now().minusDays(3));
            int deletedIdle = db.deleteUploadedIdleBefore(LocalDateTime.now().minusDays(3));
            if (deleted > 0 || deletedIdle > 0) {
                System.out.println("Cleaned " + deleted + " activity + " + deletedIdle + " idle records");
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
        Config config = ConfigLoader.load();
        new Application(config).start();
    }
}