package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public class StockItemDto {
    private Long id;

    @NotBlank
    private String productName;

    private String stockGroup;
    private String modelName;
    private String unit;

    @NotNull
    @PositiveOrZero
    private BigDecimal vatRate;

    @NotNull
    @PositiveOrZero
    private BigDecimal purchasePrice;

    @NotNull
    @PositiveOrZero
    private BigDecimal salePrice;

    @NotNull
    @PositiveOrZero
    private BigDecimal stockIn;

    @NotNull
    @PositiveOrZero
    private BigDecimal stockOut;

    private BigDecimal currentStock;
    private BigDecimal totalPurchaseValue;
    private BigDecimal totalSaleValue;

    public static StockItemDto fromEntity(StockItem entity) {
        StockItemDto dto = new StockItemDto();
        dto.setId(entity.getId());
        dto.setProductName(entity.getProductName());
        dto.setStockGroup(entity.getStockGroup());
        dto.setModelName(entity.getModelName());
        dto.setUnit(entity.getUnit());
        dto.setVatRate(entity.getVatRate());
        dto.setPurchasePrice(entity.getPurchasePrice());
        dto.setSalePrice(entity.getSalePrice());
        dto.setStockIn(entity.getStockIn());
        dto.setStockOut(entity.getStockOut());
        dto.setCurrentStock(entity.getCurrentStock());
        dto.setTotalPurchaseValue(entity.getCurrentStock().multiply(entity.getPurchasePrice()));
        dto.setTotalSaleValue(entity.getCurrentStock().multiply(entity.getSalePrice()));
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getStockGroup() { return stockGroup; }
    public void setStockGroup(String stockGroup) { this.stockGroup = stockGroup; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
    public BigDecimal getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(BigDecimal purchasePrice) { this.purchasePrice = purchasePrice; }
    public BigDecimal getSalePrice() { return salePrice; }
    public void setSalePrice(BigDecimal salePrice) { this.salePrice = salePrice; }
    public BigDecimal getStockIn() { return stockIn; }
    public void setStockIn(BigDecimal stockIn) { this.stockIn = stockIn; }
    public BigDecimal getStockOut() { return stockOut; }
    public void setStockOut(BigDecimal stockOut) { this.stockOut = stockOut; }
    public BigDecimal getCurrentStock() { return currentStock; }
    public void setCurrentStock(BigDecimal currentStock) { this.currentStock = currentStock; }
    public BigDecimal getTotalPurchaseValue() { return totalPurchaseValue; }
    public void setTotalPurchaseValue(BigDecimal totalPurchaseValue) { this.totalPurchaseValue = totalPurchaseValue; }
    public BigDecimal getTotalSaleValue() { return totalSaleValue; }
    public void setTotalSaleValue(BigDecimal totalSaleValue) { this.totalSaleValue = totalSaleValue; }
}
