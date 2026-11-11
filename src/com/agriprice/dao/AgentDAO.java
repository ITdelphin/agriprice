package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.Agent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AgentDAO {

    public Agent findByEmail(String email) throws SQLException {
        String sql = "SELECT agent_id, full_name, email, phone, password_hash, password_salt, status, created_at "
                   + "FROM agents WHERE email = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Agent findById(int agentId) throws SQLException {
        String sql = "SELECT agent_id, full_name, email, phone, password_hash, password_salt, status, created_at "
                   + "FROM agents WHERE agent_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, agentId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Agent> getAllAgents() throws SQLException {
        List<Agent> list = new ArrayList<>();
        String sql = "SELECT agent_id, full_name, email, phone, password_hash, password_salt, status, created_at "
                   + "FROM agents ORDER BY full_name ASC";
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    public int createAgent(Agent agent) throws SQLException {
        String sql = "INSERT INTO agents (full_name, email, phone, password_hash, password_salt, status) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, agent.getFullName());
            ps.setString(2, agent.getEmail().trim().toLowerCase());
            ps.setString(3, agent.getPhone());
            ps.setString(4, agent.getPasswordHash());
            ps.setString(5, agent.getPasswordSalt());
            ps.setString(6, agent.getStatus() != null ? agent.getStatus() : "ACTIVE");
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    agent.setAgentId(gk.getInt(1));
                    return agent.getAgentId();
                }
            }
        }
        return -1;
    }

    private Agent mapRow(ResultSet rs) throws SQLException {
        Agent a = new Agent();
        a.setAgentId(rs.getInt("agent_id"));
        a.setFullName(rs.getString("full_name"));
        a.setEmail(rs.getString("email"));
        a.setPhone(rs.getString("phone"));
        a.setPasswordHash(rs.getString("password_hash"));
        a.setPasswordSalt(rs.getString("password_salt"));
        a.setStatus(rs.getString("status"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        return a;
    }
}
