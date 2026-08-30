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

    public String classify(String app, String url) {
        if (isBrowser(app)) {
            return classifyByUrl(url);
        }
        return classifyByApp(app);
    }

    private String classifyByApp(String app) {
        if (app == null) {
            return "其他";
        }
        String lower = app.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : categories.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (keyword != null && lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return entry.getKey();
                }
            }
        }
        return "其他";
    }

    private String classifyByUrl(String url) {
        if (url == null || url.isBlank()) {
            return "其他";
        }
        String lower = url.toLowerCase(Locale.ROOT);
        for (Map.Entry<String, List<String>> entry : urlRules.entrySet()) {
            for (String keyword : entry.getValue()) {
                if (keyword != null && lower.contains(keyword.toLowerCase(Locale.ROOT))) {
                    return entry.getKey();
                }
            }
        }
        return "浏览网页";
    }
}