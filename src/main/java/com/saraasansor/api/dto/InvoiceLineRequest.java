package com.saraasansor.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class InvoiceLineRequest {

    @NotBlank(message = "productName is required")
    private String productName;

    @NotNull(message = "quantity is required")
    @DecimalMin(value = "0.0001", inclusive = true, message = "quantity must be greater than zero")
    private BigDecimal quantity;

    @NotNull(message = "unitPrice is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "unitPrice must be zero or positive")
    private BigDecimal unitPrice;

    @NotNull(message = "vatRate is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "vatRate must be zero or positive")
    private BigDecimal vatRate;

    public InvoiceLineRequest() {
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }
}
