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

/**
 * Manages JDBC Database Connections for AgriPrice Rwanda.
 * Reads connection settings dynamically from web.xml context parameters.
 * Implements ServletContextListener to automatically configure itself upon deployment.
 */
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
        LOGGER.info("AgriPrice Rwanda DatabaseConnection initialized from web.xml context parameters.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("AgriPrice Rwanda DatabaseConnection shutting down.");
    }

    /**
     * Configures database settings from ServletContext init-parameters.
     */
    public static synchronized void configureFromContext(ServletContext context) {
        if (context != null) {
            String url = context.getInitParameter("db.url");
            String user = context.getInitParameter("db.username");
            String pass = context.getInitParameter("db.password");
            String thresholdStr = context.getInitParameter("outlier.threshold");

            if (url != null && !url.trim().isEmpty()) {
                dbUrl = url.trim();
            }
            if (user != null && !user.trim().isEmpty()) {
                dbUsername = user.trim();
            }
            if (pass != null) {
                dbPassword = pass.trim();
            }
            if (thresholdStr != null && !thresholdStr.trim().isEmpty()) {
                try {
                    outlierThreshold = Double.parseDouble(thresholdStr.trim());
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid outlier.threshold parameter in web.xml, keeping default: " + outlierThreshold);
                }
            }
        }
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
     * Resolves configuration with environment/system property fallbacks if not yet initialized.
     */
    private static void ensureConfigured() {
        if (dbUrl == null) {
            dbUrl = System.getProperty("db.url", 
                "jdbc:mysql://localhost:3306/agriprice?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }
        if (dbUsername == null) {
            dbUsername = System.getProperty("db.username", "agriprice_user");
        }
        if (dbPassword == null) {
            dbPassword = System.getProperty("db.password", "AgriPrice@2026");
        }
    }

    /**
     * Obtains an active connection from DriverManager using the configured parameters.
     */
    public static Connection getConnection() throws SQLException {
        ensureConfigured();
        return DriverManager.getConnection(dbUrl, dbUsername, dbPassword);
    }

    public static String getDbUrl() {
        ensureConfigured();
        return dbUrl;
    }

    public static String getDbUsername() {
        ensureConfigured();
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
