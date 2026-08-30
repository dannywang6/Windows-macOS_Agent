package com.studyagent.monitor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;

public class MacNativeHelper {

    public static final class Snapshot {
        public final String name;
        public final String title;
        public final double idleSeconds;

        Snapshot(JsonNode root) {
            this.name = root.path("name").asText(null);
            this.title = root.path("title").asText(null);
            this.idleSeconds = root.path("idle").asDouble(0);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String BINARY_RESOURCE = "/native/frontmost";
    private static Path binaryPath;

    private static Snapshot cached;
    private static long cachedAt;
    private static final long CACHE_MILLIS = 2000;

    private MacNativeHelper() {
    }

    private static synchronized Path binary() throws IOException {
        if (binaryPath != null && Files.exists(binaryPath)) {
            return binaryPath;
        }
        Path dir = Files.createTempDirectory("study-agent-native");
        Path target = dir.resolve("frontmost");
        try (InputStream in = MacNativeHelper.class.getResourceAsStream(BINARY_RESOURCE)) {
            if (in == null) {
                throw new IOException("找不到打包的 macOS 助手: " + BINARY_RESOURCE);
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
        target.toFile().setExecutable(true);
        binaryPath = target;
        return binaryPath;
    }

    public static synchronized Snapshot getSnapshot() {
        long now = System.currentTimeMillis();
        if (cached != null && now - cachedAt < CACHE_MILLIS) {
            return cached;
        }
        JsonNode root = run();
        if (root != null) {
            cached = new Snapshot(root);
            cachedAt = now;
        } else if (cached != null) {
            return cached;
        }
        return cached;
    }

    private static JsonNode run() {
        try {
            Path bin = binary();
            ProcessBuilder pb = new ProcessBuilder(bin.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String out = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                return null;
            }
            return MAPPER.readTree(out);
        } catch (Exception e) {
            return null;
        }
    }
}
