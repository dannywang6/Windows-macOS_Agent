package com.studyagent.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class ConfigLoader {

    private static final String DEFAULT_RESOURCE = "/config/default-config.json";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static Path locate() {
        try {
            Path jarPath = Paths.get(
                    ConfigLoader.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .toAbsolutePath();
            Path jarDir = jarPath.getParent();
            if (jarDir != null && Files.exists(jarPath) && jarPath.getFileName().toString().endsWith(".jar")) {
                return jarDir.resolve("config.json");
            }
        } catch (Exception ignored) {
        }
        return Paths.get("config.json");
    }

    public static Config load() throws IOException {
        Path path = locate();
        if (Files.exists(path)) {
            return MAPPER.readValue(path.toFile(), Config.class);
        }
        createDefaultConfig(path);
        return MAPPER.readValue(path.toFile(), Config.class);
    }

    public static void save(Path path, Config config) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), config);
    }

    private static void createDefaultConfig(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (InputStream in = ConfigLoader.class.getResourceAsStream(DEFAULT_RESOURCE)) {
            if (in == null) {
                throw new IOException("找不到默认配置文件: " + DEFAULT_RESOURCE);
            }
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}