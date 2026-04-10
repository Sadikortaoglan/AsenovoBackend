package com.saraasansor.api.dto;

import com.saraasansor.api.model.BankAccount;

public class BankListItemResponse {

    private Long id;
    private String name;
    private String branchName;
    private String iban;
    private String currencyCode;
    private String currencyName;
    private Boolean active;

    public static BankListItemResponse fromEntity(BankAccount bank) {
        BankListItemResponse response = new BankListItemResponse();
        response.setId(bank.getId());
        response.setName(bank.getName());
        response.setBranchName(bank.getBranchName());
        response.setIban(bank.getIban());
        response.setCurrencyCode(bank.getCurrency() != null ? bank.getCurrency().name() : null);
        response.setCurrencyName(bank.getCurrency() != null ? bank.getCurrency().getDisplayName() : null);
        response.setActive(bank.getActive());
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

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
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
}
