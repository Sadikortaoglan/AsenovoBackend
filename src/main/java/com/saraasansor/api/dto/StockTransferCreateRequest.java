package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class StockTransferCreateRequest {

    @NotNull(message = "date is required")
    @JsonAlias({"transferDate"})
    private LocalDate date;

    @NotNull(message = "stockId is required")
    @JsonAlias({"productId", "partId"})
    private Long stockId;

    @NotNull(message = "outgoingWarehouseId is required")
    @JsonAlias({"fromWarehouseId", "outWarehouseId", "sourceWarehouseId"})
    private Long outgoingWarehouseId;

    @NotNull(message = "incomingWarehouseId is required")
    @JsonAlias({"inWarehouseId", "toWarehouseId", "targetWarehouseId"})
    private Long incomingWarehouseId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be greater than or equal to 1")
    @JsonAlias({"miktar"})
    private Integer quantity;

    @Size(max = 5000, message = "description must be at most 5000 characters")
    private String description;

    private Boolean active = true;

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

    public Long getOutgoingWarehouseId() {
        return outgoingWarehouseId;
    }

    public void setOutgoingWarehouseId(Long outgoingWarehouseId) {
        this.outgoingWarehouseId = outgoingWarehouseId;
    }

    public Long getIncomingWarehouseId() {
        return incomingWarehouseId;
    }

    public void setIncomingWarehouseId(Long incomingWarehouseId) {
        this.incomingWarehouseId = incomingWarehouseId;
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
}
