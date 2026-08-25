package com.studyagent.storage;

import com.studyagent.model.ActivityRecord;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SQLiteManager {

    private final String dbPath;
    private Connection connection;

    public SQLiteManager(String dbPath) {
        this.dbPath = dbPath;
    }

    public void init() throws SQLException {
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

    public void save(ActivityRecord record) throws SQLException {
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

    public List<ActivityRecord> findUnuploaded() throws SQLException {
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

    public void markUploaded(long id) throws SQLException {
        String sql = "UPDATE activity_record SET uploaded = 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public int deleteUploadedBefore(LocalDateTime deadline) throws SQLException {
        String sql = """
                DELETE FROM activity_record
                WHERE uploaded = 1 AND start_time < ?
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, deadline.toString());
            return ps.executeUpdate();
        }
    }

    public void close() throws SQLException {
        if (connection != null) {
            connection.close();
        }
    }
}