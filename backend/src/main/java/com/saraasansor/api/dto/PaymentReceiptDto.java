package com.saraasansor.api.dto;

import com.saraasansor.api.model.PaymentReceipt;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class PaymentReceiptDto {
    private Long id;
    
    @NotNull(message = "Maintenance ID cannot be empty")
    private Long maintenanceId;
    
    @NotNull(message = "Amount cannot be empty")
    @Positive(message = "Amount must be positive")
    private Double amount;
    
    @NotBlank(message = "Payer name cannot be empty")
    private String payerName;
    
    private LocalDate date;
    private String note;
    private LocalDateTime createdAt;

    public PaymentReceiptDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getMaintenanceId() {
        return maintenanceId;
    }

    public void setMaintenanceId(Long maintenanceId) {
        this.maintenanceId = maintenanceId;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public static PaymentReceiptDto fromEntity(PaymentReceipt receipt) {
        PaymentReceiptDto dto = new PaymentReceiptDto();
        dto.setId(receipt.getId());
        dto.setMaintenanceId(receipt.getMaintenance().getId());
        dto.setAmount(receipt.getAmount());
        dto.setPayerName(receipt.getPayerName());
        dto.setDate(receipt.getDate());
        dto.setNote(receipt.getNote());
        dto.setCreatedAt(receipt.getCreatedAt());
        return dto;
    }
}
