package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockUnit;

import java.time.LocalDateTime;

public class StockUnitResponse {

    private Long id;
    private String name;
    private String abbreviation;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static StockUnitResponse fromEntity(StockUnit stockUnit) {
        StockUnitResponse response = new StockUnitResponse();
        response.setId(stockUnit.getId());
        response.setName(stockUnit.getName());
        response.setAbbreviation(stockUnit.getAbbreviation());
        response.setActive(stockUnit.getActive());
        response.setCreatedAt(stockUnit.getCreatedAt());
        response.setUpdatedAt(stockUnit.getUpdatedAt());
        response.setCreatedBy(stockUnit.getCreatedBy());
        response.setUpdatedBy(stockUnit.getUpdatedBy());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
