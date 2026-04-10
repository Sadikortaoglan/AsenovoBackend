package com.saraasansor.api.dto;

import com.saraasansor.api.model.VatRate;

import java.math.BigDecimal;

public class VatRateLookupDto {

    private Long id;
    private BigDecimal rate;

    public static VatRateLookupDto fromEntity(VatRate vatRate) {
        VatRateLookupDto dto = new VatRateLookupDto();
        dto.setId(vatRate.getId());
        dto.setRate(vatRate.getRate());
        return dto;
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
}
