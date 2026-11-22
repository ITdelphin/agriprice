package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.AuditLog;
import com.agriprice.model.PriceReport;
import com.agriprice.util.InvalidPriceException;
import com.agriprice.util.PriceOutlierException;
import com.agriprice.util.UnknownProductException;
import com.agriprice.util.ValidationUtil;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * PriceDAO manages price reporting operations, enforces business rules
 * (zero price, duplicate daily submissions, 7-day trailing outlier checks),
 * and executes ACID transactions with rollbacks.
 */
public class PriceDAO {

    private static final int HIDDEN_MARKET_ID = 4;
    private final AuditLogDAO auditLogDAO = new AuditLogDAO();

    /**
     * Submits a new price report along with an audit log entry in a single atomic database transaction.
     * Enforces price validation, duplicate checking, and 7-day outlier checks.
     */
    public int submitPriceWithAudit(PriceReport report, AuditLog audit) 
            throws SQLException, InvalidPriceException, PriceOutlierException, UnknownProductException {
        
        // 1. Business Rule: Price must be > 0
        ValidationUtil.validatePrice(report.getPriceRwf());
        requireProduct(report.getProductId());

        // 2. Business Rule: Check duplicate daily submission
        if (checkDuplicateSubmission(report.getMarketId(), report.getProductId(), report.getReportDate())) {
            throw new SQLException("Duplicate submission: A price report for product ID " 
                + report.getProductId() + " in market ID " + report.getMarketId() 
                + " on " + report.getReportDate() + " already exists.");
        }

        // 3. Business Rule: 7-day Outlier Detection
        double avg7Days = getRecentAveragePrice(report.getProductId(), report.getMarketId(), 7, report.getReportDate());
        double threshold = DatabaseConnection.getOutlierThreshold();
        if (avg7Days > 0) {
            ValidationUtil.validatePriceOutlier(report.getPriceRwf(), avg7Days, threshold);
        }

        // 4. ACID Transaction
        String insertSql = "INSERT INTO price_reports (product_id, market_id, agent_id, unit, price_rwf, report_date) "
                         + "VALUES (?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // Begin Transaction

            int reportId = -1;
            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, report.getProductId());
                ps.setInt(2, report.getMarketId());
                ps.setInt(3, report.getAgentId());
                ps.setString(4, report.getUnit());
                ps.setDouble(5, report.getPriceRwf());
                ps.setDate(6, report.getReportDate());
                ps.executeUpdate();

                try (ResultSet gk = ps.getGeneratedKeys()) {
                    if (gk.next()) {
                        reportId = gk.getInt(1);
                        report.setReportId(reportId);
                    }
                }
            }

            if (reportId <= 0) {
                conn.rollback();
                throw new SQLException("Failed to retrieve generated report ID during submission.");
            }

            // Save Audit Log in same transaction
            if (audit != null) {
                audit.setReportId(reportId);
                audit.setAction("INSERT_PRICE");
                auditLogDAO.logAction(conn, audit);
            }

            conn.commit(); // Commit Transaction
            return reportId;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback(); // Rollback Transaction on any failure
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Updates an existing price report with an audit record in a single atomic transaction.
     */
    public boolean updatePriceWithAudit(PriceReport report, AuditLog audit) 
            throws SQLException, InvalidPriceException, PriceOutlierException, UnknownProductException {
        
        ValidationUtil.validatePrice(report.getPriceRwf());
        requireProduct(report.getProductId());

        double avg7Days = getRecentAveragePrice(report.getProductId(), report.getMarketId(), 7, report.getReportDate());
        double threshold = DatabaseConnection.getOutlierThreshold();
        if (avg7Days > 0) {
            ValidationUtil.validatePriceOutlier(report.getPriceRwf(), avg7Days, threshold);
        }

        String sql = "UPDATE price_reports SET unit = ?, price_rwf = ?, updated_at = CURRENT_TIMESTAMP "
                   + "WHERE report_id = ? AND agent_id = ?";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            boolean updated = false;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, report.getUnit());
                ps.setDouble(2, report.getPriceRwf());
                ps.setInt(3, report.getReportId());
                ps.setInt(4, report.getAgentId());
                updated = ps.executeUpdate() > 0;
            }

            if (!updated) {
                conn.rollback();
                return false;
            }

            if (audit != null) {
                audit.setReportId(report.getReportId());
                audit.setAction("UPDATE_PRICE");
                auditLogDAO.logAction(conn, audit);
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Deletes a price report where permitted (by the authoring agent) with audit logging.
     */
    public boolean deletePrice(int reportId, int agentId, AuditLog audit) throws SQLException {
        String sql = "DELETE FROM price_reports WHERE report_id = ? AND agent_id = ?";
        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            boolean deleted;
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, reportId);
                ps.setInt(2, agentId);
                deleted = ps.executeUpdate() > 0;
            }

            if (deleted && audit != null) {
                audit.setReportId(reportId);
                audit.setAction("DELETE_PRICE");
                auditLogDAO.logAction(conn, audit);
            }

            conn.commit();
            return deleted;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * Checks if a price report already exists for the market, product, and date combination.
     */
    public boolean checkDuplicateSubmission(int marketId, int productId, Date reportDate) throws SQLException {
        String sql = "SELECT COUNT(*) FROM price_reports WHERE market_id = ? AND product_id = ? AND report_date = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, marketId);
            ps.setInt(2, productId);
            ps.setDate(3, reportDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private void requireProduct(int productId) throws SQLException, UnknownProductException {
        String sql = "SELECT 1 FROM products WHERE product_id = ? AND status = 'ACTIVE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new UnknownProductException("The selected product is not available.");
            }
        }
    }

    /**
     * Calculates the trailing average price over the previous N days prior to a target date.
     */
    public double getRecentAveragePrice(int productId, int marketId, int days, Date referenceDate) throws SQLException {
        String sql = "SELECT AVG(price_rwf) FROM price_reports "
                   + "WHERE product_id = ? AND market_id = ? "
                   + "  AND report_date < ? "
                   + "  AND report_date >= DATE_SUB(?, INTERVAL ? DAY)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, marketId);
            ps.setDate(3, referenceDate);
            ps.setDate(4, referenceDate);
            ps.setInt(5, days > 0 ? days : 7);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? 0.0 : avg;
                }
            }
        }
        return 0.0;
    }

    public PriceReport findById(int reportId) throws SQLException {
        String sql = "SELECT r.report_id, r.product_id, r.market_id, r.agent_id, r.unit, r.price_rwf, "
                   + "       r.report_date, r.created_at, r.updated_at, "
                   + "       p.product_name, p.category AS product_category, "
                   + "       m.market_name, m.district AS market_district, "
                   + "       a.full_name AS agent_name "
                   + "FROM price_reports r "
                   + "JOIN products p ON r.product_id = p.product_id "
                   + "JOIN markets m ON r.market_id = m.market_id "
                   + "JOIN agents a ON r.agent_id = a.agent_id "
                   + "WHERE r.report_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, reportId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowWithJoins(rs);
                }
            }
        }
        return null;
    }

    /**
     * Retrieves filtered price reports with optional search, product, market, and date filters.
     */
    public List<PriceReport> getFilteredPrices(Integer productId, Integer marketId, Date date, String search) throws SQLException {
        List<PriceReport> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT r.report_id, r.product_id, r.market_id, r.agent_id, r.unit, r.price_rwf, "
          + "       r.report_date, r.created_at, r.updated_at, "
          + "       p.product_name, p.category AS product_category, "
          + "       m.market_name, m.district AS market_district, "
          + "       a.full_name AS agent_name "
          + "FROM price_reports r "
          + "JOIN products p ON r.product_id = p.product_id "
          + "JOIN markets m ON r.market_id = m.market_id "
          + "JOIN agents a ON r.agent_id = a.agent_id "
          + "WHERE 1=1 AND r.market_id <> " + HIDDEN_MARKET_ID + " "
        );

        List<Object> params = new ArrayList<>();
        if (productId != null && productId > 0) {
            sql.append(" AND r.product_id = ? ");
            params.add(productId);
        }
        if (marketId != null && marketId > 0) {
            sql.append(" AND r.market_id = ? ");
            params.add(marketId);
        }
        if (date != null) {
            sql.append(" AND r.report_date = ? ");
            params.add(date);
        }
        if (search != null && !search.trim().isEmpty()) {
            sql.append(" AND (p.product_name LIKE ? OR m.market_name LIKE ? OR p.category LIKE ?) ");
            String wild = "%" + search.trim() + "%";
            params.add(wild);
            params.add(wild);
            params.add(wild);
        }

        sql.append(" ORDER BY r.report_date DESC, r.created_at DESC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithJoins(rs));
                }
            }
        }
        return list;
    }

    /**
     * Retrieves the latest price reports for public and dashboard display.
     */
    public List<PriceReport> getLatestPrices(int limit) throws SQLException {
        List<PriceReport> list = new ArrayList<>();
        String sql = "SELECT r.report_id, r.product_id, r.market_id, r.agent_id, r.unit, r.price_rwf, "
                   + "       r.report_date, r.created_at, r.updated_at, "
                   + "       p.product_name, p.category AS product_category, "
                   + "       m.market_name, m.district AS market_district, "
                   + "       a.full_name AS agent_name "
                   + "FROM price_reports r "
                   + "JOIN products p ON r.product_id = p.product_id "
                   + "JOIN markets m ON r.market_id = m.market_id "
                    + "JOIN agents a ON r.agent_id = a.agent_id "
                    + "WHERE r.market_id <> " + HIDDEN_MARKET_ID + " "
                    + "ORDER BY r.report_date DESC, r.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithJoins(rs));
                }
            }
        }
        return list;
    }

    public List<PriceReport> getReportsByAgent(int agentId, int limit) throws SQLException {
        List<PriceReport> list = new ArrayList<>();
        String sql = "SELECT r.report_id, r.product_id, r.market_id, r.agent_id, r.unit, r.price_rwf, "
                   + "       r.report_date, r.created_at, r.updated_at, "
                   + "       p.product_name, p.category AS product_category, "
                   + "       m.market_name, m.district AS market_district, "
                   + "       a.full_name AS agent_name "
                   + "FROM price_reports r "
                   + "JOIN products p ON r.product_id = p.product_id "
                   + "JOIN markets m ON r.market_id = m.market_id "
                    + "JOIN agents a ON r.agent_id = a.agent_id "
                    + "WHERE r.agent_id = ? AND r.market_id <> " + HIDDEN_MARKET_ID + " "
                    + "ORDER BY r.report_date DESC, r.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            ps.setInt(2, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithJoins(rs));
                }
            }
        }
        return list;
    }

    public int getTotalReportsCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM price_reports WHERE market_id <> " + HIDDEN_MARKET_ID;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    /**
     * Returns historical price trends for Chart 1 (Line chart).
     */
    public List<Map<String, Object>> getHistoricalTrends(int productId, Integer marketId, int days) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        int boundedDays = Math.max(1, Math.min(days, 365));
        StringBuilder sql = new StringBuilder(
            "SELECT report_date, AVG(price_rwf) AS avg_price, MIN(price_rwf) AS min_price, MAX(price_rwf) AS max_price "
          +             "FROM price_reports "
          + "WHERE product_id = ? AND market_id <> " + HIDDEN_MARKET_ID + " "
        );
        if (marketId != null && marketId > 0) {
            sql.append(" AND market_id = ? ");
        }
        sql.append(" AND report_date >= DATE_SUB(CURDATE(), INTERVAL ? DAY) ");
        sql.append("GROUP BY report_date ORDER BY report_date ASC");

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            ps.setInt(1, productId);
            if (marketId != null && marketId > 0) {
                ps.setInt(2, marketId);
            }
            ps.setInt(marketId != null && marketId > 0 ? 3 : 2, boundedDays);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", rs.getDate("report_date").toString());
                    map.put("avgPrice", rs.getDouble("avg_price"));
                    map.put("minPrice", rs.getDouble("min_price"));
                    map.put("maxPrice", rs.getDouble("max_price"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * Returns market comparison statistics for Chart 2 (Bar chart).
     */
    public List<Map<String, Object>> getMarketComparison(int productId, Date date) throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT m.market_id, m.market_name, m.district, r.price_rwf, r.report_date, r.unit "
                   + "FROM price_reports r "
                   + "JOIN markets m ON r.market_id = m.market_id "
                   + "WHERE r.product_id = ? AND r.market_id <> " + HIDDEN_MARKET_ID
                   + " AND r.report_date = COALESCE(?, "
                   + "(SELECT MAX(report_date) FROM price_reports WHERE product_id = ? AND market_id <> " + HIDDEN_MARKET_ID + ")) "
                   + "ORDER BY r.price_rwf ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setDate(2, date);
            ps.setInt(3, productId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("marketId", rs.getInt("market_id"));
                    map.put("marketName", rs.getString("market_name"));
                    map.put("district", rs.getString("district"));
                    map.put("price", rs.getDouble("price_rwf"));
                    map.put("unit", rs.getString("unit"));
                    list.add(map);
                }
            }
        }
        return list;
    }

    /**
     * Returns submission distribution across markets for Chart 3 (Pie chart).
     */
    public List<Map<String, Object>> getMarketSubmissionDistribution() throws SQLException {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = "SELECT m.market_name, COUNT(r.report_id) AS report_count "
                   + "FROM markets m "
                   + "LEFT JOIN price_reports r ON m.market_id = r.market_id "
                   + "WHERE m.market_id <> " + HIDDEN_MARKET_ID + " "
                   + "GROUP BY m.market_id, m.market_name "
                   + "ORDER BY report_count DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("marketName", rs.getString("market_name"));
                map.put("count", rs.getInt("report_count"));
                list.add(map);
            }
        }
        return list;
    }

    public Date getLatestReportDate() throws SQLException {
        String sql = "SELECT MAX(report_date) FROM price_reports WHERE market_id <> " + HIDDEN_MARKET_ID;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getDate(1) : null;
        }
    }

    public long getLatestReportUpdatedAt(Date reportDate) throws SQLException {
        String sql = "SELECT MAX(updated_at) FROM price_reports WHERE report_date = ? AND market_id <> " + HIDDEN_MARKET_ID;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, reportDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    java.sql.Timestamp updated = rs.getTimestamp(1);
                    return updated == null ? 0L : updated.getTime();
                }
            }
        }
        return 0L;
    }

    /**
     * Mandatory Assessment Demonstration: Shows that PreparedStatement safely treats
     * attempted SQL injection payloads as literal strings rather than executable SQL syntax.
     */
    public List<PriceReport> demonstratePreparedStatementDefense(String attemptedInjectionPayload) throws SQLException {
        List<PriceReport> list = new ArrayList<>();
        // Even if input is "' OR '1'='1", PreparedStatement parameterization prevents injection.
        String sql = "SELECT r.report_id, r.product_id, r.market_id, r.agent_id, r.unit, r.price_rwf, "
                   + "       r.report_date, r.created_at, r.updated_at, "
                   + "       p.product_name, p.category AS product_category, "
                   + "       m.market_name, m.district AS market_district, "
                   + "       a.full_name AS agent_name "
                   + "FROM price_reports r "
                   + "JOIN products p ON r.product_id = p.product_id "
                   + "JOIN markets m ON r.market_id = m.market_id "
                    + "JOIN agents a ON r.agent_id = a.agent_id "
                    + "WHERE p.product_name = ? AND r.market_id <> " + HIDDEN_MARKET_ID;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, attemptedInjectionPayload);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowWithJoins(rs));
                }
            }
        }
        return list;
    }

    private PriceReport mapRowWithJoins(ResultSet rs) throws SQLException {
        PriceReport r = new PriceReport();
        r.setReportId(rs.getInt("report_id"));
        r.setProductId(rs.getInt("product_id"));
        r.setMarketId(rs.getInt("market_id"));
        r.setAgentId(rs.getInt("agent_id"));
        r.setUnit(rs.getString("unit"));
        r.setPriceRwf(rs.getDouble("price_rwf"));
        r.setReportDate(rs.getDate("report_date"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setUpdatedAt(rs.getTimestamp("updated_at"));
        r.setProductName(rs.getString("product_name"));
        r.setProductCategory(rs.getString("product_category"));
        r.setMarketName(rs.getString("market_name"));
        r.setMarketDistrict(rs.getString("market_district"));
        r.setAgentName(rs.getString("agent_name"));
        return r;
    }
}
