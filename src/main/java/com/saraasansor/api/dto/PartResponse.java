package com.saraasansor.api.dto;

import com.saraasansor.api.model.Part;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PartResponse {

    private Long id;
    private String name;
    private String code;
    private String barcode;
    private Long vatRateId;
    private BigDecimal vatRate;
    private Long stockGroupId;
    private String stockGroupName;
    private Long unitId;
    private String unitName;
    private Long brandId;
    private String brandName;
    private Long modelId;
    private String modelName;
    private Double purchasePrice;
    private Double salePrice;
    private String description;
    private String imagePath;
    private Integer stock;
    private Integer stockEntry;
    private Integer stockExit;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static PartResponse fromEntity(Part part) {
        PartResponse response = new PartResponse();
        response.setId(part.getId());
        response.setName(part.getName());
        response.setCode(part.getCode());
        response.setBarcode(part.getBarcode());
        response.setVatRateId(part.getVatRate() != null ? part.getVatRate().getId() : null);
        response.setVatRate(part.getVatRate() != null ? part.getVatRate().getRate() : null);
        response.setStockGroupId(part.getStockGroup() != null ? part.getStockGroup().getId() : null);
        response.setStockGroupName(part.getStockGroup() != null ? part.getStockGroup().getName() : null);
        response.setUnitId(part.getUnit() != null ? part.getUnit().getId() : null);
        response.setUnitName(part.getUnit() != null ? part.getUnit().getName() : null);
        response.setBrandId(part.getBrand() != null ? part.getBrand().getId() : null);
        response.setBrandName(part.getBrand() != null ? part.getBrand().getName() : null);
        response.setModelId(part.getModel() != null ? part.getModel().getId() : null);
        response.setModelName(part.getModel() != null ? part.getModel().getName() : null);
        response.setPurchasePrice(part.getPurchasePrice());
        response.setSalePrice(part.getUnitPrice());
        response.setDescription(part.getDescription());
        response.setImagePath(part.getImagePath());
        response.setStock(part.getStock());
        response.setStockEntry(part.getStockEntry() != null ? part.getStockEntry() : (part.getStock() != null ? part.getStock() : 0));
        response.setStockExit(part.getStockExit() != null ? part.getStockExit() : 0);
        response.setActive(part.getActive());
        response.setCreatedAt(part.getCreatedAt());
        response.setUpdatedAt(part.getUpdatedAt());
        response.setCreatedBy(part.getCreatedBy());
        response.setUpdatedBy(part.getUpdatedBy());
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

    public Long getStockGroupId() {
        return stockGroupId;
    }

    public void setStockGroupId(Long stockGroupId) {
        this.stockGroupId = stockGroupId;
    }

    public String getStockGroupName() {
        return stockGroupName;
    }

    public void setStockGroupName(String stockGroupName) {
        this.stockGroupName = stockGroupName;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public String getBrandName() {
        return brandName;
    }

    public void setBrandName(String brandName) {
        this.brandName = brandName;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
