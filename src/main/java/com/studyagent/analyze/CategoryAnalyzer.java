package com.studyagent.analyze;

import java.util.Locale;

public class CategoryAnalyzer {

    public String analyze(String app, String title, String url) {
        if (app == null) {
            return "未知";
        }
        String lowerApp = app.toLowerCase(Locale.ROOT);

        if (lowerApp.contains("idea") || lowerApp.contains("code")
                || lowerApp.contains("gpt") || lowerApp.contains("navicat")) {
            return "编程学习";
        }
        if (isGame(lowerApp)) {
            return "游戏";
        }
        if (isBrowser(lowerApp)) {
            return analyzeUrl(url);
        }
        return "其他";
    }

    public boolean isBrowser(String app) {
        if (app == null) {
            return false;
        }
        String lower = app.toLowerCase(Locale.ROOT);
        return lower.contains("chrome") || lower.contains("edge") || lower.contains("firefox")
                || lower.contains("safari");
    }

    private boolean isGame(String app) {
        return app.contains("fortnite")
                || app.contains("steam")
                || app.contains("epicgameslauncher")
                || app.contains("tslgame")
                || app.contains("cs2")
                || app.contains("call of duty")
                || app.contains("callofduty")
                || app.contains("warzone")
                || app.contains("blackops")
                || app.contains("battlefield")
                || app.contains("bf1")
                || app.contains("bf4")
                || app.contains("bfv")
                || app.contains("bf2042")
                || app.contains("gta")
                || app.contains("overwatch");
    }

    private String analyzeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "其他";
        }
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.contains("github.com") || lower.contains("stackoverflow.com")) {
            return "编程学习";
        }
        if (lower.contains("youtube.com") || lower.contains("bilibili.com")) {
            return "娱乐";
        }
        return "浏览网页";
    }
}