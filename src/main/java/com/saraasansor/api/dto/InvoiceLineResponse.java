package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BUnitInvoiceLine;

import java.math.BigDecimal;

public class InvoiceLineResponse {

    private Long id;
    private String productName;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal vatRate;
    private BigDecimal lineSubTotal;
    private BigDecimal lineVatTotal;
    private BigDecimal lineGrandTotal;

    public InvoiceLineResponse() {
    }

    public static InvoiceLineResponse fromEntity(B2BUnitInvoiceLine line) {
        InvoiceLineResponse response = new InvoiceLineResponse();
        response.setId(line.getId());
        response.setProductName(line.getProductName());
        response.setQuantity(line.getQuantity());
        response.setUnitPrice(line.getUnitPrice());
        response.setVatRate(line.getVatRate());
        response.setLineSubTotal(line.getLineSubTotal());
        response.setLineVatTotal(line.getLineVatTotal());
        response.setLineGrandTotal(line.getLineGrandTotal());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BigDecimal getLineSubTotal() {
        return lineSubTotal;
    }

    public void setLineSubTotal(BigDecimal lineSubTotal) {
        this.lineSubTotal = lineSubTotal;
    }

    public BigDecimal getLineVatTotal() {
        return lineVatTotal;
    }

    public void setLineVatTotal(BigDecimal lineVatTotal) {
        this.lineVatTotal = lineVatTotal;
    }

    public BigDecimal getLineGrandTotal() {
        return lineGrandTotal;
    }

    public void setLineGrandTotal(BigDecimal lineGrandTotal) {
        this.lineGrandTotal = lineGrandTotal;
    }
}
