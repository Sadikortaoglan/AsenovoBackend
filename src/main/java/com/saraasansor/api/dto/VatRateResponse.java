package com.saraasansor.api.dto;

import com.saraasansor.api.model.VatRate;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VatRateResponse {

    private Long id;
    private BigDecimal rate;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static VatRateResponse fromEntity(VatRate vatRate) {
        VatRateResponse response = new VatRateResponse();
        response.setId(vatRate.getId());
        response.setRate(vatRate.getRate());
        response.setActive(vatRate.getActive());
        response.setCreatedAt(vatRate.getCreatedAt());
        response.setUpdatedAt(vatRate.getUpdatedAt());
        response.setCreatedBy(vatRate.getCreatedBy());
        response.setUpdatedBy(vatRate.getUpdatedBy());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
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
