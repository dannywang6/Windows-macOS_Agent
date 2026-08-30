package com.studyagent.analyze;

import com.studyagent.config.Config;

public class CategoryAnalyzer {

    private final Config config;

    public CategoryAnalyzer(Config config) {
        this.config = config;
    }

    public String analyze(String app, String title, String url) {
        return config.classify(app, url);
    }

    public boolean isBrowser(String app) {
        return config.isBrowser(app);
    }
}
