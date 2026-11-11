package com.agriprice.dao;

import com.agriprice.config.DatabaseConnection;
import com.agriprice.model.AuditLog;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class AuditLogDAO {

    /**
     * Inserts an audit log record using an existing transactional Connection.
     * Essential for atomic price + audit commits.
     */
    public int logAction(Connection conn, AuditLog log) throws SQLException {
        String sql = "INSERT INTO audit_log (agent_id, report_id, action, old_value, new_value) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (log.getAgentId() != null) {
                ps.setInt(1, log.getAgentId());
            } else {
                ps.setNull(1, java.sql.Types.INTEGER);
            }
            if (log.getReportId() != null) {
                ps.setInt(2, log.getReportId());
            } else {
                ps.setNull(2, java.sql.Types.INTEGER);
            }
            ps.setString(3, log.getAction());
            ps.setString(4, log.getOldValue());
            ps.setString(5, log.getNewValue());
            ps.executeUpdate();
            try (ResultSet gk = ps.getGeneratedKeys()) {
                if (gk.next()) {
                    log.setAuditId(gk.getInt(1));
                    return log.getAuditId();
                }
            }
        }
        return -1;
    }

    public List<AuditLog> getRecentLogs(int limit) throws SQLException {
        List<AuditLog> list = new ArrayList<>();
        String sql = "SELECT a.audit_id, a.agent_id, a.report_id, a.action, a.old_value, a.new_value, a.action_date, "
                   + "       ag.full_name AS agent_name "
                   + "FROM audit_log a "
                   + "LEFT JOIN agents ag ON a.agent_id = ag.agent_id "
                   + "ORDER BY a.action_date DESC LIMIT ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit > 0 ? limit : 20);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    AuditLog al = new AuditLog();
                    al.setAuditId(rs.getInt("audit_id"));
                    int agId = rs.getInt("agent_id");
                    al.setAgentId(rs.wasNull() ? null : agId);
                    int repId = rs.getInt("report_id");
                    al.setReportId(rs.wasNull() ? null : repId);
                    al.setAction(rs.getString("action"));
                    al.setOldValue(rs.getString("old_value"));
                    al.setNewValue(rs.getString("new_value"));
                    al.setActionDate(rs.getTimestamp("action_date"));
                    al.setAgentName(rs.getString("agent_name"));
                    list.add(al);
                }
            }
        }
        return list;
    }
}
