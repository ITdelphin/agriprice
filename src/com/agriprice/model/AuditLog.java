package com.agriprice.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class AuditLog implements Serializable {
    private static final long serialVersionUID = 1L;

    private int auditId;
    private Integer agentId;
    private Integer reportId;
    private String action;
    private String oldValue;
    private String newValue;
    private Timestamp actionDate;

    // Joined view field
    private String agentName;

    public AuditLog() {}

    public AuditLog(int auditId, Integer agentId, Integer reportId, String action, String oldValue, String newValue, Timestamp actionDate) {
        this.auditId = auditId;
        this.agentId = agentId;
        this.reportId = reportId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.actionDate = actionDate;
    }

    public int getAuditId() {
        return auditId;
    }

    public void setAuditId(int auditId) {
        this.auditId = auditId;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public Integer getReportId() {
        return reportId;
    }

    public void setReportId(Integer reportId) {
        this.reportId = reportId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public Timestamp getActionDate() {
        return actionDate;
    }

    public void setActionDate(Timestamp actionDate) {
        this.actionDate = actionDate;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }
}
