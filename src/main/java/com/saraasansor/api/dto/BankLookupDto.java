package com.saraasansor.api.dto;

import com.saraasansor.api.model.BankAccount;

public class BankLookupDto {

    private Long id;
    private String name;
    private String branchName;
    private String currencyCode;

    public static BankLookupDto fromEntity(BankAccount bank) {
        BankLookupDto dto = new BankLookupDto();
        dto.setId(bank.getId());
        dto.setName(bank.getName());
        dto.setBranchName(bank.getBranchName());
        dto.setCurrencyCode(bank.getCurrency() != null ? bank.getCurrency().name() : null);
        return dto;
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

    public String getCurrencyCode() {
        return currencyCode;
    }

    public void setCurrencyCode(String currencyCode) {
        this.currencyCode = currencyCode;
    }
}
