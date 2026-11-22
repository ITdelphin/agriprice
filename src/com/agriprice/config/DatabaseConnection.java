package com.agriprice.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;


@WebListener
public class DatabaseConnection implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());
    private static final String DRIVER_CLASS = "com.mysql.cj.jdbc.Driver";

    private static String dbUrl;
    private static String dbUsername;
    private static String dbPassword;
    private static double outlierThreshold = 25.0; // default 25%

    static {
        try {
            Class.forName(DRIVER_CLASS);
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "MySQL JDBC Driver not found on classpath: " + DRIVER_CLASS, e);
        }
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        configureFromContext(context);
        LOGGER.info("AgriPrice Rwanda DatabaseConnection initialized from environment or web.xml context parameters.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("AgriPrice Rwanda DatabaseConnection shutting down.");
    }

    /**
     * Configures database settings from ServletContext init-parameters.
     */
    public static synchronized void configureFromContext(ServletContext context) {
        dbUrl = resolveSetting(context, "db.url", "AGRIPRICE_DB_URL");
        dbUsername = resolveSetting(context, "db.username", "AGRIPRICE_DB_USERNAME");
        dbPassword = resolveSetting(context, "db.password", "AGRIPRICE_DB_PASSWORD");

        String thresholdStr = context != null ? context.getInitParameter("outlier.threshold") : null;
        if (thresholdStr != null && !thresholdStr.trim().isEmpty()) {
            try {
                outlierThreshold = Double.parseDouble(thresholdStr.trim());
            } catch (NumberFormatException e) {
                LOGGER.warning("Invalid outlier.threshold parameter, keeping default: " + outlierThreshold);
            }
        }
    }

    private static String resolveSetting(ServletContext context, String paramName, String envName) {
        String value = context != null ? context.getInitParameter(paramName) : null;
        if (value == null || value.trim().isEmpty()) {
            value = System.getenv(envName);
        }
        if (value == null || value.trim().isEmpty()) {
            value = System.getProperty(envName);
        }
        String trimmed = value == null ? "" : value.trim();
        if (trimmed.matches("\\$\\{[^}]+\\}")) {
            String key = trimmed.substring(2, trimmed.length() - 1);
            value = System.getenv(key);
            if (value == null || value.trim().isEmpty()) {
                value = System.getProperty(key);
            }
            trimmed = value == null ? "" : value.trim();
        }
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Manual configuration method for standalone testing or CLI verification.
     */
    public static synchronized void configure(String url, String username, String password, double threshold) {
        dbUrl = url;
        dbUsername = username;
        dbPassword = password;
        outlierThreshold = threshold;
    }

    /**
     * Obtains an active connection from DriverManager using the configured parameters.
     */
    public static Connection getConnection() throws SQLException {
        if (dbUrl == null || dbUsername == null || dbPassword == null) {
            throw new SQLException("Database settings are missing. Set AGRIPRICE_DB_URL, AGRIPRICE_DB_USERNAME and AGRIPRICE_DB_PASSWORD.");
        }
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    public static String getDbUrl() {
        return dbUrl;
    }

    public static String getDbUsername() {
        return dbUsername;
    }

    public static double getOutlierThreshold() {
        return outlierThreshold;
    }

    public static void main(String[] args) {
        System.out.println("Testing DatabaseConnection...");
        try (Connection conn = getConnection()) {
            if (conn != null && !conn.isClosed()) {
                System.out.println("SUCCESS: Connected to MySQL database via DatabaseConnection!");
                System.out.println("URL: " + getDbUrl());
                System.out.println("User: " + getDbUsername());
                System.out.println("Outlier Threshold: " + getOutlierThreshold() + "%");
            }
        } catch (SQLException e) {
            System.err.println("FAILURE: Database connection failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
