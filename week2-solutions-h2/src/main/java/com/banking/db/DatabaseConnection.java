package com.banking.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

/**
 * Singleton class to manage H2 database connections.
 * Replaces file-based storage from week1 with database storage.
 */
public class DatabaseConnection {
    private static final String CONFIG_FILE = "application.properties";
    private static DatabaseConnection instance;
    private final String url;
    private final String username;
    private final String password;

    private DatabaseConnection() {
        Properties props = loadProperties();
        this.url = props.getProperty("db.url");
        this.username = props.getProperty("db.username");
        this.password = props.getProperty("db.password");

        // Initialize database schema
        try (Connection conn = getConnection()) {
            initializeDatabase(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }

    /**
     * Get a database connection.
     * Simple connection management for learning purposes.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    private Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new RuntimeException("Unable to find " + CONFIG_FILE);
            }
            props.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading database properties", e);
        }
        return props;
    }

    private void initializeDatabase(Connection conn) {
        try {
            // Execute schema.sql to create tables
            try (InputStream schemaStream = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
                if (schemaStream == null) {
                    throw new RuntimeException("Unable to find schema.sql");
                }
                String schema = new String(schemaStream.readAllBytes());
                // Split and execute each statement separately
                for (String statement : schema.split(";")) {
                    if (!statement.trim().isEmpty()) {
                        conn.createStatement().execute(statement);
                    }
                }
            }
            System.out.println("Database schema initialized successfully");
        } catch (IOException | SQLException e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
