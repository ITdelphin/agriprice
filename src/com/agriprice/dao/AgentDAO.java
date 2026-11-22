package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.Agent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class AgentDAO {

    public Agent findByEmail(String email) throws SQLException {
        String sql = "SELECT agent_id, full_name, email, phone, password_hash, password_salt, created_at "
                   + "FROM agents WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public Agent findById(int agentId) throws SQLException {
        String sql = "SELECT agent_id, full_name, email, phone, password_hash, password_salt, created_at "
                   + "FROM agents WHERE agent_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        }
    }

    public int getAgentCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM agents";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Agent mapRow(ResultSet rs) throws SQLException {
        Agent a = new Agent();
        a.setAgentId(rs.getInt("agent_id"));
        a.setFullName(rs.getString("full_name"));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setPasswordSalt(rs.getString("password_salt"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        return a;
    }
}
