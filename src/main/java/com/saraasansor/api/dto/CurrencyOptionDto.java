package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BCurrency;

public class CurrencyOptionDto {

    private String code;
    private String displayName;

    public CurrencyOptionDto() {
    }

    public CurrencyOptionDto(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public static CurrencyOptionDto fromCurrency(B2BCurrency currency) {
        return new CurrencyOptionDto(currency.name(), currency.getDisplayName());
    }
}
