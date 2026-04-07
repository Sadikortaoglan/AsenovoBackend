package com.saraasansor.api.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class VatRateUpdateRequest {

    @NotNull(message = "rate is required")
    @DecimalMin(value = "0.00", inclusive = true, message = "rate cannot be negative")
    @DecimalMax(value = "100.00", inclusive = true, message = "rate must be less than or equal to 100")
    private BigDecimal rate;

    private Boolean active;

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
}
