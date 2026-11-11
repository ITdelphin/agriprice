package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.Watchlist;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * WatchlistDAO manages anonymous visitor watchlists and target price alert evaluations.
 */
public class WatchlistDAO {

    public int addToWatchlist(Watchlist item) throws SQLException {
        String sql = "INSERT INTO watchlist (visitor_session_id, product_id, market_id, target_price) "
                   + "VALUES (?, ?, ?, ?) "
                   + "ON DUPLICATE KEY UPDATE target_price = VALUES(target_price)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, item.getVisitorSessionId());
            ps.setInt(2, item.getProductId());
            if (item.getMarketId() != null && item.getMarketId() > 0) {
                ps.setInt(3, item.getMarketId());
            } else {
                ps.setNull(3, java.sql.Types.INTEGER);
            }
            ps.setDouble(4, item.getTargetPrice());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    item.setWatchlistId(gk.getInt(1));
                    return item.getWatchlistId();
                }
            }
        }
        return -1;
    }

    public List<Watchlist> getBySession(String visitorSessionId) throws SQLException {
        List<Watchlist> list = new ArrayList<>();
        String sql = "SELECT w.watchlist_id, w.visitor_session_id, w.product_id, w.market_id, w.target_price, w.created_at, "
                   + "       p.product_name, p.category, p.default_unit, "
                   + "       m.market_name, "
                   + "       (SELECT pr.price_rwf FROM price_reports pr "
                   + "        WHERE pr.product_id = w.product_id "
                   + "          AND (w.market_id IS NULL OR pr.market_id = w.market_id) "
                   + "        ORDER BY pr.report_date DESC, pr.created_at DESC LIMIT 1) AS latest_price "
                   + "FROM watchlist w "
                   + "JOIN products p ON w.product_id = p.product_id "
                   + "LEFT JOIN markets m ON w.market_id = m.market_id "
                   + "WHERE w.visitor_session_id = ? "
                   + "ORDER BY w.created_at DESC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, visitorSessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Watchlist w = new Watchlist();
                    w.setWatchlistId(rs.getInt("watchlist_id"));
                    w.setVisitorSessionId(rs.getString("visitor_session_id"));
                    w.setProductId(rs.getInt("product_id"));
                    int mId = rs.getInt("market_id");
                    w.setMarketId(rs.wasNull() ? null : mId);
                    w.setTargetPrice(rs.getDouble("target_price"));
                    w.setCreatedAt(rs.getTimestamp("created_at"));
                    w.setProductName(rs.getString("product_name"));
                    w.setCategory(rs.getString("category"));
                    w.setDefaultUnit(rs.getString("default_unit"));
                    w.setMarketName(rs.getString("market_name"));
                    double lp = rs.getDouble("latest_price");
                    w.setCurrentPrice(rs.wasNull() ? 0.0 : lp);
                    // Alert condition: Current price reached or fell below target price
                    w.setTargetReached(w.getCurrentPrice() > 0 && w.getCurrentPrice() <= w.getTargetPrice());
                    list.add(w);
                }
            }
        }
        return list;
    }

    public boolean updateTargetPrice(int watchlistId, String visitorSessionId, double targetPrice) throws SQLException {
        String sql = "UPDATE watchlist SET target_price = ? WHERE watchlist_id = ? AND visitor_session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, targetPrice);
            ps.setInt(2, watchlistId);
            ps.setString(3, visitorSessionId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteItem(int watchlistId, String visitorSessionId) throws SQLException {
        String sql = "DELETE FROM watchlist WHERE watchlist_id = ? AND visitor_session_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, watchlistId);
            ps.setString(2, visitorSessionId);
            return ps.executeUpdate() > 0;
        }
    }
}
