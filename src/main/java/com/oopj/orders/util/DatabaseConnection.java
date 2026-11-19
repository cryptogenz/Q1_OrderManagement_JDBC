package com.oopj.orders.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class DatabaseConnection {
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream input = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (input == null) {
                throw new IllegalStateException("db.properties was not found in src/main/resources.");
            }
            PROPERTIES.load(input);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() { }

    public static Connection getConnection() throws SQLException {
        String url = required("db.url");
        String user = required("db.username");
        String password = PROPERTIES.getProperty("db.password", "");
        return DriverManager.getConnection(url, user, password);
    }

    private static String required(String key) {
        String value = PROPERTIES.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing setting in db.properties: " + key);
        }
        return value.trim();
    }
}
