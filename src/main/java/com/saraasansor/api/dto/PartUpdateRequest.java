package com.saraasansor.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class PartUpdateRequest {

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    private String name;

    @Size(max = 100, message = "code must be at most 100 characters")
    private String code;

    @Size(max = 100, message = "barcode must be at most 100 characters")
    private String barcode;

    @NotNull(message = "vatRateId is required")
    private Long vatRateId;

    @NotNull(message = "stockGroupId is required")
    private Long stockGroupId;

    @NotNull(message = "unitId is required")
    private Long unitId;

    private Long brandId;

    private Long modelId;

    @DecimalMin(value = "0.0", message = "purchasePrice must be greater than or equal to 0")
    private Double purchasePrice;

    @NotNull(message = "salePrice is required")
    @DecimalMin(value = "0.0", message = "salePrice must be greater than or equal to 0")
    private Double salePrice;

    @Size(max = 5000, message = "description must be at most 5000 characters")
    private String description;

    @Size(max = 500, message = "imagePath must be at most 500 characters")
    private String imagePath;

    @Min(value = 0, message = "stock must be greater than or equal to 0")
    private Integer stock;

    @Min(value = 0, message = "stockEntry must be greater than or equal to 0")
    private Integer stockEntry;

    @Min(value = 0, message = "stockExit must be greater than or equal to 0")
    private Integer stockExit;

    private Boolean active;

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

    public Long getStockGroupId() {
        return stockGroupId;
    }

    public void setStockGroupId(Long stockGroupId) {
        this.stockGroupId = stockGroupId;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public Long getModelId() {
        return modelId;
    }

    public void setModelId(Long modelId) {
        this.modelId = modelId;
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
}
