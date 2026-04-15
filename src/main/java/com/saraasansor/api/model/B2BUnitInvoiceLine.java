package com.saraasansor.api.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "b2b_unit_invoice_lines")
public class B2BUnitInvoiceLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private B2BUnitInvoice invoice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id")
    private Part stock;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 14, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "vat_rate", nullable = false, precision = 7, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "line_sub_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineSubTotal;

    @Column(name = "line_vat_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineVatTotal;

    @Column(name = "line_grand_total", nullable = false, precision = 14, scale = 2)
    private BigDecimal lineGrandTotal;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    public B2BUnitInvoiceLine() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public B2BUnitInvoice getInvoice() {
        return invoice;
    }

    public void setInvoice(B2BUnitInvoice invoice) {
        this.invoice = invoice;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public Part getStock() {
        return stock;
    }

    public void setStock(Part stock) {
        this.stock = stock;
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

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
