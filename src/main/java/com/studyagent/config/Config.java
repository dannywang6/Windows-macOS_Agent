package com.studyagent.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Data
public class Config {

    private Server server = new Server();
    private Monitor monitor = new Monitor();
    private int tabServerPort = 9876;
    private int guiPort = 18080;
    private String dbPath = "data/activity.db";

    private List<String> browsers = new ArrayList<>();

    private Map<String, List<String>> categories = new LinkedHashMap<>();

    private Map<String, List<String>> urlRules = new LinkedHashMap<>();

    private Map<String, List<String>> titleRules = new LinkedHashMap<>();

    @Data
    public static class Server {
        private String baseUrl = "http://localhost:8080";
        private String reportPath = "/api/activity/report";
    }

    @Data
    public static class Monitor {
        private int intervalSeconds = 5;
        private int idleThresholdSeconds = 60;
    }

    @JsonIgnore
    public long getIntervalMillis() {
        return Duration.ofSeconds(monitor.getIntervalSeconds()).toMillis();
    }

    @JsonIgnore
    public long getIdleThresholdMillis() {
        return Duration.ofSeconds(monitor.getIdleThresholdSeconds()).toMillis();
    }

    public boolean isBrowser(String app) {
        if (app == null) {
            return false;
        }
        String lower = app.toLowerCase(Locale.ROOT);
        for (String b : browsers) {
            if (b != null && lower.contains(b.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /** 分类结果：主分类 + 子分类 */
    public static class Classification {
        public String category;
        public String subcategory;

        public Classification(String category, String subcategory) {
            this.category = category;
            this.subcategory = subcategory;
        }
    }

    /** 综合 app/title/url 判断主分类与子分类 */
    public Classification analyze(String app, String title, String url) {
        if (isBrowser(app)) {
            return analyzeBrowser(title, url);
        }
        return analyzeApp(app);
    }

    /** 浏览器：优先按 title 关键字，其次按 url 规则 */
    private Classification analyzeBrowser(String title, String url) {
        // 1) 先看标题里的关键字，可按内容细分（如 youtube 上的 java 教程 -> 学习）
        String titleCat = matchRules(titleRules, title);
        if (titleCat != null) {
            return new Classification(titleCat, title);
        }
        // 2) 再看 url 规则
        String urlCat = matchRules(urlRules, url);
        if (urlCat != null) {
            return new Classification(urlCat, null);
        }
        return new Classification("浏览网页", null);
    }

    /** 非浏览器应用：按 app 名称 */
    private Classification analyzeApp(String app) {
        String cat = matchRules(categories, app);
        if (cat == null) {
            return new Classification("其他", null);
        }
        return new Classification(cat, null);
    }

    private String matchRules(Map<String, List<String>> rules, String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : rules.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (keyword != null && lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
}