package com.studyagent.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConfigClassificationTest {

    private Config config() {
        Config c = new Config();
        c.getBrowsers().addAll(java.util.List.of("chrome", "edge", "safari"));
        c.getCategories().put("学习", java.util.List.of("idea", "pycharm"));
        c.getCategories().put("娱乐", java.util.List.of("steam", "bilibili"));
        c.getUrlRules().put("学习", java.util.List.of("github.com", "leetcode.com"));
        c.getUrlRules().put("娱乐", java.util.List.of("youtube.com", "bilibili.com"));
        c.getTitleRules().put("学习", java.util.List.of("java", "教程", "编程"));
        c.getTitleRules().put("娱乐", java.util.List.of("搞笑", "动漫"));
        return c;
    }

    @Test
    void nonBrowserClassifyByApp() {
        Config.Classification cl = config().analyze("idea", "Main.java", null);
        assertEquals("学习", cl.category);
        assertEquals(null, cl.subcategory);
    }

    @Test
    void browserClassifyByUrl() {
        Config.Classification cl = config().analyze("chrome", null, "https://github.com/foo");
        assertEquals("学习", cl.category);
    }

    @Test
    void youtubeJavaTutorialShouldBeLearning() {
        // chrome + youtube + 标题含 java -> 学习（覆盖娱乐域名）
        Config.Classification cl = config().analyze("chrome", "Java多线程教程 - YouTube", "https://www.youtube.com/watch?v=abc");
        assertEquals("学习", cl.category);
        assertEquals("Java多线程教程 - YouTube", cl.subcategory);
    }

    @Test
    void youtubeFunVideoShouldBeEntertainment() {
        Config.Classification cl = config().analyze("chrome", "搞笑合集 - YouTube", "https://www.youtube.com/watch?v=xyz");
        assertEquals("娱乐", cl.category);
    }

    @Test
    void bilibiliJavaTutorialShouldBeLearning() {
        // 真实场景：bilibili 域名 + 标题含 Java（韩顺平教程）
        Config.Classification cl = config().analyze("chrome",
                "0007_韩顺平Java_Java故事_哔哩哔哩_bilibili",
                "https://www.bilibili.com/video/BV1fh411y7R8/?spm_id_from=333.788");
        assertEquals("学习", cl.category);
        assertEquals("0007_韩顺平Java_Java故事_哔哩哔哩_bilibili", cl.subcategory);
    }

    @Test
    void browserUnmatchedUrlFallsBackToBrowsing() {
        Config.Classification cl = config().analyze("safari", null, "https://random-site.com");
        assertEquals("浏览网页", cl.category);
    }
}