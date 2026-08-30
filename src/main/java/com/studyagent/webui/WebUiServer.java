package com.studyagent.webui;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.studyagent.config.Config;
import com.studyagent.config.ConfigLoader;
import com.studyagent.storage.SQLiteManager;
import com.studyagent.util.LogBuffer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

public class WebUiServer {

    private final ObjectMapper mapper = createMapper();
    private final SQLiteManager db;
    private final LogBuffer logBuffer;
    private final Runnable uploadNow;

    private static ObjectMapper createMapper() {
        ObjectMapper m = new ObjectMapper();
        m.registerModule(new JavaTimeModule());
        m.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return m;
    }

    public WebUiServer(SQLiteManager db, LogBuffer logBuffer, Runnable uploadNow) {
        this.db = db;
        this.logBuffer = logBuffer;
        this.uploadNow = uploadNow;
    }

    public void start(int port) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/api/config", this::handleConfig);
        server.createContext("/api/records", this::handleRecords);
        server.createContext("/api/stats", this::handleStats);
        server.createContext("/api/logs", this::handleLogs);
        server.createContext("/api/upload-now", this::handleUploadNow);
        server.createContext("/", this::handleStatic);

        server.setExecutor(Executors.newCachedThreadPool());
        server.start();
        System.out.println("Web UI 已启动，访问 http://localhost:" + port);
    }

    private void handleConfig(HttpExchange ex) throws IOException {
        try {
            if ("GET".equals(ex.getRequestMethod())) {
                Config config = ConfigLoader.load();
                writeJson(ex, 200, config);
            } else if ("PUT".equals(ex.getRequestMethod())) {
                String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                Config config = mapper.readValue(body, Config.class);
                ConfigLoader.save(ConfigLoader.locate(), config);
                writeJson(ex, 200, Map.of("ok", true));
            } else {
                writeJson(ex, 405, Map.of("error", "方法不支持"));
            }
        } catch (Exception e) {
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleRecords(HttpExchange ex) throws IOException {
        try {
            String path = ex.getRequestURI().getPath();
            if (path.endsWith("/count")) {
                writeJson(ex, 200, Map.of("count", db.countRecords()));
                return;
            }
            if (path.endsWith("/today")) {
                writeJson(ex, 200, db.aggregateToday());
                return;
            }
            Map<String, String> query = parseQuery(ex.getRequestURI().getRawQuery());
            int limit = parseInt(query.get("limit"), 100);
            int offset = parseInt(query.get("offset"), 0);
            writeJson(ex, 200, db.findRecords(limit, offset));
        } catch (SQLException e) {
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            writeJson(ex, 500, Map.of("error", "内部错误: " + e));
        }
    }

    private void handleStats(HttpExchange ex) throws IOException {
        try {
            writeJson(ex, 200, db.aggregateByCategory());
        } catch (SQLException e) {
            writeJson(ex, 500, Map.of("error", e.getMessage()));
        }
    }

    private void handleLogs(HttpExchange ex) throws IOException {
        writeJson(ex, 200, logBuffer.snapshot());
    }

    private void handleUploadNow(HttpExchange ex) throws IOException {
        if ("POST".equals(ex.getRequestMethod())) {
            try {
                uploadNow.run();
                writeJson(ex, 200, Map.of("ok", true));
            } catch (Exception e) {
                writeJson(ex, 500, Map.of("error", e.getMessage()));
            }
        } else {
            writeJson(ex, 405, Map.of("error", "方法不支持"));
        }
    }

    private void handleStatic(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        if (path == null || path.equals("/")) {
            path = "/index.html";
        }
        String resource = "/webui" + path;
        try (InputStream in = WebUiServer.class.getResourceAsStream(resource)) {
            if (in == null) {
                writeText(ex, 404, "资源不存在: " + path + "（前端尚未构建，部署到 resources/webui 即可）");
                return;
            }
            byte[] data = in.readAllBytes();
            String contentType = guessContentType(path);
            ex.getResponseHeaders().set("Content-Type", contentType);
            ex.sendResponseHeaders(200, data.length);
            try (OutputStream os = ex.getResponseBody()) {
                os.write(data);
            }
        }
    }

    private static String guessContentType(String path) {
        if (path.endsWith(".html")) return "text/html; charset=utf-8";
        if (path.endsWith(".js")) return "application/javascript";
        if (path.endsWith(".css")) return "text/css";
        if (path.endsWith(".json")) return "application/json";
        if (path.endsWith(".svg")) return "image/svg+xml";
        if (path.endsWith(".png")) return "image/png";
        if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
        if (path.endsWith(".ico")) return "image/x-icon";
        if (path.endsWith(".woff")) return "font/woff";
        if (path.endsWith(".woff2")) return "font/woff2";
        return "application/octet-stream";
    }

    private static Map<String, String> parseQuery(String raw) {
        Map<String, String> map = new HashMap<>();
        if (raw == null) return map;
        for (String pair : raw.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(kv[0], kv[1]);
            }
        }
        return map;
    }

    private static int parseInt(String s, int def) {
        if (s == null) return def;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private void writeJson(HttpExchange ex, int code, Object body) throws IOException {
        byte[] data = mapper.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }

    private void writeText(HttpExchange ex, int code, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(code, data.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(data);
        }
    }
}
