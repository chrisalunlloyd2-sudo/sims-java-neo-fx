package com.aigen.sims;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

public class SQLiteMemory {
    private static final String DB_URL = "jdbc:sqlite:aegis_ledger.db";

    public SQLiteMemory() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.out.println("[MEMORY ERROR] SQLite JDBC Driver not found.");
        }
        initDatabase();
    }

    private void initDatabase() {
        System.out.println("[MEMORY] Bootstrapping SQLite Persistent State...");
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS AgentMemory (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "agent_name TEXT," +
                         "memory_type TEXT," +
                         "data TEXT," +
                         "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");
                         
            stmt.execute("CREATE TABLE IF NOT EXISTS TopologicalHomology (" +
                         "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                         "node_a TEXT, node_b TEXT, weight REAL)");

            System.out.println(" -> Schema active.");
        } catch (Exception e) {
            System.out.println("[MEMORY ERROR] " + e.getMessage());
        }
    }

    public void logMemory(String agent, String type, String data) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement pstmt = conn.prepareStatement("INSERT INTO AgentMemory (agent_name, memory_type, data) VALUES (?, ?, ?)")) {
            pstmt.setString(1, agent);
            pstmt.setString(2, type);
            pstmt.setString(3, data);
            pstmt.executeUpdate();
        } catch (Exception e) {
            System.out.println("[MEMORY ERROR] " + e.getMessage());
        }
    }
}
