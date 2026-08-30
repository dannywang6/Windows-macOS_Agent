package com.studyagent.storage;

import com.studyagent.model.ActivityRecord;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SQLiteManager {

    private final String dbPath;
    private Connection connection;

    public SQLiteManager(String dbPath) {
        this.dbPath = dbPath;
    }

    public void init() throws SQLException {
        ensureParentDir();
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS activity_record (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT,
                        app             TEXT,
                        title           TEXT,
                        url             TEXT,
                        category        TEXT,
                        start_time      TEXT,
                        duration_millis INTEGER,
                        uploaded        INTEGER DEFAULT 0
                    )
                    """);
        }
    }

    public synchronized void save(ActivityRecord record) throws SQLException {
        String sql = """
                INSERT INTO activity_record (app, title, url, category, start_time, duration_millis, uploaded)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getApp());
            ps.setString(2, record.getTitle());
            ps.setString(3, record.getUrl());
            ps.setString(4, record.getCategory());
            ps.setString(5, record.getStartTime().toString());
            ps.setLong(6, record.getDurationMillis());
            ps.setInt(7, record.isUploaded() ? 1 : 0);
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    record.setId(keys.getLong(1));
                }
            }
        }
    }

    public synchronized List<ActivityRecord> findUnuploaded() throws SQLException {
        String sql = "SELECT * FROM activity_record WHERE uploaded = 0";
        List<ActivityRecord> records = new ArrayList<>();

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ActivityRecord record = new ActivityRecord();
                record.setId(rs.getLong("id"));
                record.setApp(rs.getString("app"));
                record.setTitle(rs.getString("title"));
                record.setUrl(rs.getString("url"));
                record.setCategory(rs.getString("category"));
                record.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
                record.setDurationMillis(rs.getLong("duration_millis"));
                record.setUploaded(rs.getInt("uploaded") == 1);
                records.add(record);
            }
        }
        return records;
    }

    public synchronized void markUploaded(long id) throws SQLException {
        String sql = "UPDATE activity_record SET uploaded = 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public synchronized int deleteUploadedBefore(LocalDateTime deadline) throws SQLException {
        String sql = """
                DELETE FROM activity_record
                WHERE uploaded = 1 AND start_time < ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, deadline.toString());
            return ps.executeUpdate();
        }
    }

    public synchronized void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }

    public synchronized List<ActivityRecord> findRecords(int limit, int offset) throws SQLException {
        String sql = """
                SELECT * FROM activity_record
                ORDER BY id DESC
                LIMIT ? OFFSET ?
                """;
        List<ActivityRecord> records = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ps.setInt(2, offset);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    records.add(map(rs));
                }
            }
        }
        return records;
    }

    public synchronized int countRecords() throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM activity_record";
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt("c") : 0;
        }
    }

    public synchronized List<Map<String, Object>> aggregateToday() throws SQLException {
        String sql = """
                SELECT app, url, category,
                       SUM(duration_millis) AS total_millis,
                       COUNT(*) AS cnt
                FROM activity_record
                WHERE start_time >= ?
                GROUP BY app, url, category
                ORDER BY total_millis DESC
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().atStartOfDay().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("app", rs.getString("app"));
                    row.put("url", rs.getString("url"));
                    row.put("category", rs.getString("category"));
                    row.put("totalMillis", rs.getLong("total_millis"));
                    row.put("count", rs.getInt("cnt"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    public synchronized List<Map<String, Object>> aggregateByCategory() throws SQLException {
        String sql = """
                SELECT category, SUM(duration_millis) AS total_millis
                FROM activity_record
                WHERE start_time >= ?
                GROUP BY category
                ORDER BY total_millis DESC
                """;
        List<Map<String, Object>> rows = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, LocalDate.now().atStartOfDay().toString());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("category", rs.getString("category"));
                    row.put("totalMillis", rs.getLong("total_millis"));
                    rows.add(row);
                }
            }
        }
        return rows;
    }

    private ActivityRecord map(ResultSet rs) throws SQLException {
        ActivityRecord record = new ActivityRecord();
        record.setId(rs.getLong("id"));
        record.setApp(rs.getString("app"));
        record.setTitle(rs.getString("title"));
        record.setUrl(rs.getString("url"));
        record.setCategory(rs.getString("category"));
        record.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
        record.setDurationMillis(rs.getLong("duration_millis"));
        record.setUploaded(rs.getInt("uploaded") == 1);
        return record;
    }

    private void ensureParentDir() {
        int idx = dbPath.lastIndexOf('/');
        if (idx < 0) {
            idx = dbPath.lastIndexOf('\\');
        }
        if (idx > 0) {
            Path dir = Paths.get(dbPath.substring(0, idx));
            try {
                java.nio.file.Files.createDirectories(dir);
            } catch (java.io.IOException e) {
                System.out.println("创建数据库目录失败: " + e.getMessage());
            }
        }
    }
}