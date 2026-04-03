package com.saraasansor.api.dto;

import com.saraasansor.api.model.CashAccount;

import java.time.LocalDateTime;

public class CashboxResponse {

    private Long id;
    private String name;
    private String currencyId;
    private String currencyCode;
    private String currencyName;
    private Boolean active;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static CashboxResponse fromEntity(CashAccount account) {
        CashboxResponse response = new CashboxResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setCurrencyId(account.getCurrency() != null ? account.getCurrency().name() : null);
        response.setCurrencyCode(account.getCurrency() != null ? account.getCurrency().name() : null);
        response.setCurrencyName(account.getCurrency() != null ? account.getCurrency().getDisplayName() : null);
        response.setActive(account.getActive());
        response.setDescription(account.getDescription());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
        response.setCreatedBy(account.getCreatedBy());
        response.setUpdatedBy(account.getUpdatedBy());
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

    public String getCurrencyId() {
        return currencyId;
    }

    public void setCurrencyId(String currencyId) {
        this.currencyId = currencyId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }

    public String getCurrencyName() {
        return currencyName;
    }

    public void setCurrencyName(String currencyName) {
        this.currencyName = currencyName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
