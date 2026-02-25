package com.saraasansor.api.dto;

import com.saraasansor.api.model.StockTransfer;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class StockTransferDto {
    private Long id;

    @NotNull
    private Long fromStockId;

    @NotNull
    private Long toStockId;

    @NotNull
    @Positive
    private BigDecimal quantity;

    @NotNull
    private LocalDateTime transferDate;

    private String note;

    public static StockTransferDto fromEntity(StockTransfer entity) {
        StockTransferDto dto = new StockTransferDto();
        dto.setId(entity.getId());
        dto.setFromStockId(entity.getFromStock().getId());
        dto.setToStockId(entity.getToStock().getId());
        dto.setQuantity(entity.getQuantity());
        dto.setTransferDate(entity.getTransferDate());
        dto.setNote(entity.getNote());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getFromStockId() { return fromStockId; }
    public void setFromStockId(Long fromStockId) { this.fromStockId = fromStockId; }
    public Long getToStockId() { return toStockId; }
    public void setToStockId(Long toStockId) { this.toStockId = toStockId; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public LocalDateTime getTransferDate() { return transferDate; }
    public void setTransferDate(LocalDateTime transferDate) { this.transferDate = transferDate; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
