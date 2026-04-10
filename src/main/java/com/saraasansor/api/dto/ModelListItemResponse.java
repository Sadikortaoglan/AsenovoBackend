package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockModel;

import java.time.LocalDateTime;

public class ModelListItemResponse {

    private Long id;
    private String name;
    private Long brandId;
    private String brandName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ModelListItemResponse fromEntity(StockModel model) {
        ModelListItemResponse response = new ModelListItemResponse();
        response.setId(model.getId());
        response.setName(model.getName());
        if (model.getBrand() != null) {
            response.setBrandId(model.getBrand().getId());
            response.setBrandName(model.getBrand().getName());
        }
        response.setActive(model.getActive());
        response.setCreatedAt(model.getCreatedAt());
        response.setUpdatedAt(model.getUpdatedAt());
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

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
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
}
