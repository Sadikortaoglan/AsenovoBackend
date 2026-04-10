package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockGroup;

import java.time.LocalDateTime;

public class StockGroupResponse {

    private Long id;
    private String name;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static StockGroupResponse fromEntity(StockGroup stockGroup) {
        StockGroupResponse response = new StockGroupResponse();
        response.setId(stockGroup.getId());
        response.setName(stockGroup.getName());
        response.setActive(stockGroup.getActive());
        response.setCreatedAt(stockGroup.getCreatedAt());
        response.setUpdatedAt(stockGroup.getUpdatedAt());
        response.setCreatedBy(stockGroup.getCreatedBy());
        response.setUpdatedBy(stockGroup.getUpdatedBy());
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
