package com.agriprice.model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Timestamp;

public class PriceReport implements Serializable {
    private static final long serialVersionUID = 1L;

    private int reportId;
    private int productId;
    private int marketId;
    private int agentId;
    private String unit;
    private double priceRwf;
    private Date reportDate;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Joined view attributes
    private String productName;
    private String productCategory;
    private String marketName;
    private String marketDistrict;
    private String agentName;

    public PriceReport() {}

    public PriceReport(int reportId, int productId, int marketId, int agentId, String unit, double priceRwf, Date reportDate) {
        this.reportId = reportId;
        this.productId = productId;
        this.marketId = marketId;
        this.agentId = agentId;
        this.unit = unit;
        this.priceRwf = priceRwf;
        this.reportDate = reportDate;
    }

    public int getReportId() {
        return reportId;
    }

    public void setReportId(int reportId) {
        this.reportId = reportId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public int getMarketId() {
        return marketId;
    }

    public void setMarketId(int marketId) {
        this.marketId = marketId;
    }

    public int getAgentId() {
        return agentId;
    }

    public void setAgentId(int agentId) {
        this.agentId = agentId;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public double getPriceRwf() {
        return priceRwf;
    }

    public void setPriceRwf(double priceRwf) {
        this.priceRwf = priceRwf;
    }

    public Date getReportDate() {
        return reportDate;
    }

    public void setReportDate(Date reportDate) {
        this.reportDate = reportDate;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public Timestamp getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Timestamp updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductCategory() {
        return productCategory;
    }

    public void setProductCategory(String productCategory) {
        this.productCategory = productCategory;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getMarketDistrict() {
        return marketDistrict;
    }

    public void setMarketDistrict(String marketDistrict) {
        this.marketDistrict = marketDistrict;
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }
}
