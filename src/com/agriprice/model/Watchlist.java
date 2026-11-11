package com.agriprice.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Watchlist implements Serializable {
    private static final long serialVersionUID = 1L;

    private int watchlistId;
    private String visitorSessionId;
    private int productId;
    private Integer marketId; // nullable
    private double targetPrice;
    private Timestamp createdAt;

    // Joined view fields
    private String productName;
    private String category;
    private String defaultUnit;
    private String marketName;
    private double currentPrice;
    private boolean targetReached;

    public Watchlist() {}

    public Watchlist(int watchlistId, String visitorSessionId, int productId, Integer marketId, double targetPrice) {
        this.watchlistId = watchlistId;
        this.visitorSessionId = visitorSessionId;
        this.productId = productId;
        this.marketId = marketId;
        this.targetPrice = targetPrice;
    }

    public int getWatchlistId() {
        return watchlistId;
    }

    public void setWatchlistId(int watchlistId) {
        this.watchlistId = watchlistId;
    }

    public String getVisitorSessionId() {
        return visitorSessionId;
    }

    public void setVisitorSessionId(String visitorSessionId) {
        this.visitorSessionId = visitorSessionId;
    }

    public int getProductId() {
        return productId;
    }

    public void setProductId(int productId) {
        this.productId = productId;
    }

    public Integer getMarketId() {
        return marketId;
    }

    public void setMarketId(Integer marketId) {
        this.marketId = marketId;
    }

    public double getTargetPrice() {
        return targetPrice;
    }

    public void setTargetPrice(double targetPrice) {
        this.targetPrice = targetPrice;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDefaultUnit() {
        return defaultUnit;
    }

    public void setDefaultUnit(String defaultUnit) {
        this.defaultUnit = defaultUnit;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public boolean isTargetReached() {
        return targetReached;
    }

    public void setTargetReached(boolean targetReached) {
        this.targetReached = targetReached;
    }
}
