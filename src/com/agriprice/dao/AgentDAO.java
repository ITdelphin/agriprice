package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.Agent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

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

    public void createPasswordResetToken(int agentId, String tokenHash, Timestamp expiresAt) throws SQLException {
        String deleteSql = "DELETE FROM password_reset_tokens WHERE agent_id = ? AND used_at IS NULL";
        String insertSql = "INSERT INTO password_reset_tokens (agent_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection()) {
            try (PreparedStatement delete = conn.prepareStatement(deleteSql)) {
                delete.setInt(1, agentId);
                delete.executeUpdate();
            }
            try (PreparedStatement insert = conn.prepareStatement(insertSql)) {
                insert.setInt(1, agentId);
                insert.setString(2, tokenHash);
                insert.setTimestamp(3, expiresAt);
                insert.executeUpdate();
            }
        }
    }

    public Integer findAgentForResetToken(String tokenHash, Timestamp now) throws SQLException {
        String sql = "SELECT agent_id FROM password_reset_tokens "
                   + "WHERE token_hash = ? AND used_at IS NULL AND expires_at > ? "
                   + "ORDER BY token_id DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.setTimestamp(2, now);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("agent_id") : null;
            }
        }
    }

    public void markResetTokenUsed(String tokenHash) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE token_hash = ? AND used_at IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        }
    }

    public void updatePassword(int agentId, String passwordHash, String passwordSalt) throws SQLException {
        String sql = "UPDATE agents SET password_hash = ?, password_salt = ? WHERE agent_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setString(2, passwordSalt);
            ps.setInt(3, agentId);
            ps.executeUpdate();
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
