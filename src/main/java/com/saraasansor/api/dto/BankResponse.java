package com.saraasansor.api.dto;

import com.saraasansor.api.model.BankAccount;

import java.time.LocalDateTime;

public class BankResponse {

    private Long id;
    private String name;
    private String branchName;
    private String accountNumber;
    private String iban;
    private String currencyId;
    private String currencyCode;
    private String currencyName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static BankResponse fromEntity(BankAccount bank) {
        BankResponse response = new BankResponse();
        response.setId(bank.getId());
        response.setName(bank.getName());
        response.setBranchName(bank.getBranchName());
        response.setAccountNumber(bank.getAccountNumber());
        response.setIban(bank.getIban());
        response.setCurrencyId(bank.getCurrency() != null ? bank.getCurrency().name() : null);
        response.setCurrencyCode(bank.getCurrency() != null ? bank.getCurrency().name() : null);
        response.setCurrencyName(bank.getCurrency() != null ? bank.getCurrency().getDisplayName() : null);
        response.setActive(bank.getActive());
        response.setCreatedAt(bank.getCreatedAt());
        response.setUpdatedAt(bank.getUpdatedAt());
        response.setCreatedBy(bank.getCreatedBy());
        response.setUpdatedBy(bank.getUpdatedBy());
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

    public String getBranchName() {
        return branchName;
    }

    public void setBranchName(String branchName) {
        this.branchName = branchName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
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
