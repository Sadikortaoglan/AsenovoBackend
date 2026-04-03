package com.saraasansor.api.dto;

import com.saraasansor.api.model.CashAccount;

import java.time.LocalDateTime;

public class CashboxListItemResponse {

    private Long id;
    private String name;
    private String currencyId;
    private String currencyCode;
    private String currencyName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CashboxListItemResponse fromEntity(CashAccount account) {
        CashboxListItemResponse response = new CashboxListItemResponse();
        response.setId(account.getId());
        response.setName(account.getName());
        response.setCurrencyId(account.getCurrency() != null ? account.getCurrency().name() : null);
        response.setCurrencyCode(account.getCurrency() != null ? account.getCurrency().name() : null);
        response.setCurrencyName(account.getCurrency() != null ? account.getCurrency().getDisplayName() : null);
        response.setActive(account.getActive());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());
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
