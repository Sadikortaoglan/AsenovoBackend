package com.saraasansor.api.dto;

import com.saraasansor.api.model.Part;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PartListItemResponse {

    private Long id;
    private String name;
    private String code;
    private String barcode;
    private Long vatRateId;
    private BigDecimal vatRate;
    private Double purchasePrice;
    private Double salePrice;
    private Integer stock;
    private Integer stockEntry;
    private Integer stockExit;
    private String stockGroupName;
    private String unitName;
    private String brandName;
    private String modelName;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PartListItemResponse fromEntity(Part part) {
        PartListItemResponse response = new PartListItemResponse();
        response.setId(part.getId());
        response.setName(part.getName());
        response.setCode(part.getCode());
        response.setBarcode(part.getBarcode());
        response.setVatRateId(part.getVatRate() != null ? part.getVatRate().getId() : null);
        response.setVatRate(part.getVatRate() != null ? part.getVatRate().getRate() : null);
        response.setPurchasePrice(part.getPurchasePrice());
        response.setSalePrice(part.getUnitPrice());
        response.setStock(part.getStock());
        response.setStockEntry(part.getStockEntry() != null ? part.getStockEntry() : (part.getStock() != null ? part.getStock() : 0));
        response.setStockExit(part.getStockExit() != null ? part.getStockExit() : 0);
        response.setStockGroupName(part.getStockGroup() != null ? part.getStockGroup().getName() : null);
        response.setUnitName(part.getUnit() != null ? part.getUnit().getName() : null);
        response.setBrandName(part.getBrand() != null ? part.getBrand().getName() : null);
        response.setModelName(part.getModel() != null ? part.getModel().getName() : null);
        response.setActive(part.getActive());
        response.setCreatedAt(part.getCreatedAt());
        response.setUpdatedAt(part.getUpdatedAt());
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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getBarcode() {
        return barcode;
    }

    public void setBarcode(String barcode) {
        this.barcode = barcode;
    }

    public Long getVatRateId() {
        return vatRateId;
    }

    public void setVatRateId(Long vatRateId) {
        this.vatRateId = vatRateId;
    }

    public BigDecimal getVatRate() {
        return vatRate;
    }

    public void setVatRate(BigDecimal vatRate) {
        this.vatRate = vatRate;
    }

    public Double getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(Double purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public Double getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(Double salePrice) {
        this.salePrice = salePrice;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getStockEntry() {
        return stockEntry;
    }

    public void setStockEntry(Integer stockEntry) {
        this.stockEntry = stockEntry;
    }

    public Integer getStockExit() {
        return stockExit;
    }

    public void setStockExit(Integer stockExit) {
        this.stockExit = stockExit;
    }

    public String getStockGroupName() {
        return stockGroupName;
    }

    public void setStockGroupName(String stockGroupName) {
        this.stockGroupName = stockGroupName;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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
}
