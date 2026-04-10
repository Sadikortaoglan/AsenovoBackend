package com.saraasansor.api.dto;

import com.saraasansor.api.model.CashAccount;

public class CashboxLookupDto {

    private Long id;
    private String name;
    private String currencyId;
    private String currencyCode;
    private String currencyName;

    public static CashboxLookupDto fromEntity(CashAccount account) {
        CashboxLookupDto dto = new CashboxLookupDto();
        dto.setId(account.getId());
        dto.setName(account.getName());
        dto.setCurrencyId(account.getCurrency() != null ? account.getCurrency().name() : null);
        dto.setCurrencyCode(account.getCurrency() != null ? account.getCurrency().name() : null);
        dto.setCurrencyName(account.getCurrency() != null ? account.getCurrency().getDisplayName() : null);
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
}
