package com.agriprice.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Agent implements Serializable {
    private static final long serialVersionUID = 1L;

    private int agentId;
    private String fullName;
    private String email;
    private String phone;
    private String passwordHash;
    private String passwordSalt;
    private String status;
    private Timestamp createdAt;

    public Agent() {}

    public Agent(int agentId, String fullName, String email, String phone, String passwordHash, String passwordSalt, String status, Timestamp createdAt) {
        this.agentId = agentId;
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.passwordHash = passwordHash;
        this.passwordSalt = passwordSalt;
        this.status = status;
        this.createdAt = createdAt;
    }

    public int getAgentId() {
        return agentId;
    }

    public void setAgentId(int agentId) {
        this.agentId = agentId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
