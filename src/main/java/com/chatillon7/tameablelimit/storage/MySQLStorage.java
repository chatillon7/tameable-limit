package com.chatillon7.tameablelimit.storage;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

// MySQL Implementation
public class MySQLStorage implements IStorage {
    private final String host, database, username, password;
    private final int port;
    private Connection connection;

    public MySQLStorage(String host, int port, String database, String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    @Override
    public void initialize() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            connection = DriverManager.getConnection(
                "jdbc:mysql://" + host + ":" + port + "/" + database, username, password);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS tameable_data (" +
                    "player_uuid VARCHAR(36)," +
                    "mob_type VARCHAR(32)," +
                    "count INT," +
                    "PRIMARY KEY (player_uuid, mob_type))"
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void saveTameableData(UUID playerUUID, Map<String, Integer> tameableCounts) {
        String sql = "REPLACE INTO tameable_data (player_uuid, mob_type, count) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            for (Map.Entry<String, Integer> entry : tameableCounts.entrySet()) {
                pstmt.setString(1, playerUUID.toString());
                pstmt.setString(2, entry.getKey());
                pstmt.setInt(3, entry.getValue());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, Integer> loadTameableData(UUID playerUUID) {
        Map<String, Integer> data = new HashMap<>();
        String sql = "SELECT mob_type, count FROM tameable_data WHERE player_uuid = ?";
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, playerUUID.toString());
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                data.put(rs.getString("mob_type"), rs.getInt("count"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
