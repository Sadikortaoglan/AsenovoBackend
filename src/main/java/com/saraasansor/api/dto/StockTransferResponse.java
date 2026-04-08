package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockTransfer;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class StockTransferResponse {

    private Long id;
    private LocalDate date;
    private Long stockId;
    private String stockName;
    private Long outgoingWarehouseId;
    private String outgoingWarehouseName;
    private Long incomingWarehouseId;
    private String incomingWarehouseName;
    private Integer quantity;
    private String description;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static StockTransferResponse fromEntity(StockTransfer transfer) {
        StockTransferResponse response = new StockTransferResponse();
        response.setId(transfer.getId());
        response.setDate(transfer.getDate());
        response.setStockId(transfer.getStock() != null ? transfer.getStock().getId() : null);
        response.setStockName(transfer.getStock() != null ? transfer.getStock().getName() : null);
        response.setOutgoingWarehouseId(transfer.getOutgoingWarehouse() != null ? transfer.getOutgoingWarehouse().getId() : null);
        response.setOutgoingWarehouseName(transfer.getOutgoingWarehouse() != null ? transfer.getOutgoingWarehouse().getName() : null);
        response.setIncomingWarehouseId(transfer.getIncomingWarehouse() != null ? transfer.getIncomingWarehouse().getId() : null);
        response.setIncomingWarehouseName(transfer.getIncomingWarehouse() != null ? transfer.getIncomingWarehouse().getName() : null);
        response.setQuantity(transfer.getQuantity());
        response.setDescription(transfer.getDescription());
        response.setActive(transfer.getActive());
        response.setCreatedAt(transfer.getCreatedAt());
        response.setUpdatedAt(transfer.getUpdatedAt());
        response.setCreatedBy(transfer.getCreatedBy());
        response.setUpdatedBy(transfer.getUpdatedBy());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public Long getStockId() {
        return stockId;
    }

    public void setStockId(Long stockId) {
        this.stockId = stockId;
    }

    public String getStockName() {
        return stockName;
    }

    public void setStockName(String stockName) {
        this.stockName = stockName;
    }

    public Long getOutgoingWarehouseId() {
        return outgoingWarehouseId;
    }

    public void setOutgoingWarehouseId(Long outgoingWarehouseId) {
        this.outgoingWarehouseId = outgoingWarehouseId;
    }

    public String getOutgoingWarehouseName() {
        return outgoingWarehouseName;
    }

    public void setOutgoingWarehouseName(String outgoingWarehouseName) {
        this.outgoingWarehouseName = outgoingWarehouseName;
    }

    public Long getIncomingWarehouseId() {
        return incomingWarehouseId;
    }

    public void setIncomingWarehouseId(Long incomingWarehouseId) {
        this.incomingWarehouseId = incomingWarehouseId;
    }

    public String getIncomingWarehouseName() {
        return incomingWarehouseName;
    }

    public void setIncomingWarehouseName(String incomingWarehouseName) {
        this.incomingWarehouseName = incomingWarehouseName;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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
