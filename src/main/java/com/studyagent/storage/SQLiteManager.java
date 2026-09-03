package com.studyagent.storage;

import com.studyagent.model.ActivityRecord;
import com.studyagent.model.IdleRecord;

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
                        subcategory     TEXT,
                        start_time      TEXT,
                        end_time        TEXT,
                        uploaded        INTEGER DEFAULT 0
                    )
                    """);
            statement.execute("""
                    CREATE TABLE IF NOT EXISTS idle_record (
                        id              INTEGER PRIMARY KEY AUTOINCREMENT,
                        activity_id     INTEGER NOT NULL,
                        start_time      TEXT,
                        end_time        TEXT,
                        uploaded        INTEGER DEFAULT 0
                    )
                    """);
        }
    }

    public synchronized void save(ActivityRecord record) throws SQLException {
        String sql = """
                INSERT INTO activity_record (app, title, url, category, subcategory, start_time, end_time, uploaded)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, record.getApp());
            ps.setString(2, record.getTitle());
            ps.setString(3, record.getUrl());
            ps.setString(4, record.getCategory());
            ps.setString(5, record.getSubcategory());
            ps.setString(6, record.getStartTime().toString());
            ps.setString(7, record.getEndTime().toString());
            ps.setInt(8, record.isUploaded() ? 1 : 0);
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
                record.setSubcategory(rs.getString("subcategory"));
                record.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
                record.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
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

    public synchronized void saveIdle(IdleRecord idle) throws SQLException {
        String sql = "INSERT INTO idle_record (activity_id, start_time, end_time, uploaded) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, idle.getActivityId());
            ps.setString(2, idle.getStartTime().toString());
            ps.setString(3, idle.getEndTime().toString());
            ps.setInt(4, idle.isUploaded() ? 1 : 0);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    idle.setId(keys.getLong(1));
                }
            }
        }
    }

    public synchronized Map<Long, List<IdleRecord>> findIdleByActivityIds(List<Long> activityIds) throws SQLException {
        Map<Long, List<IdleRecord>> result = new LinkedHashMap<>();
        if (activityIds.isEmpty()) {
            return result;
        }
        StringBuilder placeholders = new StringBuilder();
        for (int i = 0; i < activityIds.size(); i++) {
            if (i > 0) placeholders.append(',');
            placeholders.append('?');
        }
        String sql = "SELECT id, activity_id, start_time, end_time, uploaded FROM idle_record WHERE activity_id IN (" + placeholders + ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            for (int i = 0; i < activityIds.size(); i++) {
                ps.setLong(i + 1, activityIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    IdleRecord idle = new IdleRecord();
                    idle.setId(rs.getLong("id"));
                    idle.setActivityId(rs.getLong("activity_id"));
                    idle.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
                    idle.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
                    idle.setUploaded(rs.getInt("uploaded") == 1);
                    result.computeIfAbsent(idle.getActivityId(), k -> new ArrayList<>()).add(idle);
                }
            }
        }
        return result;
    }

    public synchronized void markIdleUploaded(long id) throws SQLException {
        String sql = "UPDATE idle_record SET uploaded = 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public synchronized int deleteUploadedIdleBefore(LocalDateTime deadline) throws SQLException {
        String sql = "DELETE FROM idle_record WHERE uploaded = 1 AND end_time < ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, deadline.toString());
            return ps.executeUpdate();
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
                       CAST((julianday(end_time) - julianday(start_time)) * 86400000 AS INTEGER) AS total_millis,
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
                SELECT category,
                       CAST((julianday(end_time) - julianday(start_time)) * 86400000 AS INTEGER) AS total_millis
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
        record.setSubcategory(rs.getString("subcategory"));
        record.setStartTime(LocalDateTime.parse(rs.getString("start_time")));
        record.setEndTime(LocalDateTime.parse(rs.getString("end_time")));
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