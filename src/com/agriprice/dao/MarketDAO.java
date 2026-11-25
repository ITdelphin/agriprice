package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.Market;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO performing full CRUD operations on the 'markets' table.
 */
public class MarketDAO {

    private static final int HIDDEN_MARKET_ID = 4;

    // 1. CREATE
    public int create(Market market) throws SQLException {
        String sql = "INSERT INTO markets (market_name, district, province, location_description, image_url) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, market.getMarketName().trim());
            ps.setString(2, market.getDistrict().trim());
            ps.setString(3, market.getProvince().trim());
            ps.setString(4, market.getLocationDescription());
            ps.setString(5, market.getImageUrl());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    market.setMarketId(gk.getInt(1));
                    return market.getMarketId();
                }
            }
        }
        return -1;
    }

    // 2. READ (Single)
    public Market findById(int marketId) throws SQLException {
        String sql = "SELECT market_id, market_name, district, province, location_description, image_url, created_at "
                   + "FROM markets WHERE market_id = ? AND market_id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, marketId);
            ps.setInt(2, HIDDEN_MARKET_ID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    // 2. READ (All)
    public List<Market> getAll() throws SQLException {
        List<Market> list = new ArrayList<>();
        String sql = "SELECT market_id, market_name, district, province, location_description, image_url, created_at "
                   + "FROM markets WHERE market_id <> ? ORDER BY market_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, HIDDEN_MARKET_ID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        }
        return list;
    }

    // 3. UPDATE
    public boolean update(Market market) throws SQLException {
        String sql = "UPDATE markets SET market_name = ?, district = ?, province = ?, location_description = ?, image_url = ? "
                   + "WHERE market_id = ? AND market_id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, market.getMarketName().trim());
            ps.setString(2, market.getDistrict().trim());
            ps.setString(3, market.getProvince().trim());
            ps.setString(4, market.getLocationDescription());
            ps.setString(5, market.getImageUrl());
            ps.setInt(6, market.getMarketId());
            ps.setInt(7, HIDDEN_MARKET_ID);
            return ps.executeUpdate() > 0;
        }
    }

    // 4. DELETE
    public boolean delete(int marketId) throws SQLException {
        String sql = "DELETE FROM markets WHERE market_id = ? AND market_id <> ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, marketId);
            ps.setInt(2, HIDDEN_MARKET_ID);
            return ps.executeUpdate() > 0;
        }
    }

    private Market mapRow(ResultSet rs) throws SQLException {
        Market m = new Market();
        m.setMarketId(rs.getInt("market_id"));
        m.setMarketName(rs.getString("market_name"));
        m.setDistrict(rs.getString("district"));
        m.setProvince(rs.getString("province"));
        m.setLocationDescription(rs.getString("location_description"));
        m.setImageUrl(rs.getString("image_url"));
        m.setCreatedAt(rs.getTimestamp("created_at"));
        return m;
    }
}
