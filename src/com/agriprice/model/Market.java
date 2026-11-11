package com.agriprice.model;

import java.io.Serializable;
import java.sql.Timestamp;

public class Market implements Serializable {
    private static final long serialVersionUID = 1L;

    private int marketId;
    private String marketName;
    private String district;
    private String province;
    private String locationDescription;
    private Timestamp createdAt;

    public Market() {}

    public Market(int marketId, String marketName, String district, String province, String locationDescription, Timestamp createdAt) {
        this.marketId = marketId;
        this.marketName = marketName;
        this.district = district;
        this.province = province;
        this.locationDescription = locationDescription;
        this.createdAt = createdAt;
    }

    public int getMarketId() {
        return marketId;
    }

    public void setMarketId(int marketId) {
        this.marketId = marketId;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getLocationDescription() {
        return locationDescription;
    }

    public void setLocationDescription(String locationDescription) {
        this.locationDescription = locationDescription;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }
}
