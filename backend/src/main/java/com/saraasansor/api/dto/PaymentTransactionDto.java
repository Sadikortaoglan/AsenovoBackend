package com.saraasansor.api.dto;

import com.saraasansor.api.model.PaymentTransaction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PaymentTransactionDto {
    private Long id;
    private Long currentAccountId;
    private Long buildingId;

    @NotBlank
    private String paymentType;

    @NotNull
    @Positive
    private BigDecimal amount;

    private String description;

    @NotNull
    private LocalDateTime paymentDate;

    private Long cashAccountId;
    private Long bankAccountId;

    public static PaymentTransactionDto fromEntity(PaymentTransaction entity) {
        PaymentTransactionDto dto = new PaymentTransactionDto();
        dto.setId(entity.getId());
        dto.setCurrentAccountId(entity.getCurrentAccount() != null ? entity.getCurrentAccount().getId() : null);
        dto.setBuildingId(entity.getBuilding() != null ? entity.getBuilding().getId() : null);
        dto.setPaymentType(entity.getPaymentType().name());
        dto.setAmount(entity.getAmount());
        dto.setDescription(entity.getDescription());
        dto.setPaymentDate(entity.getPaymentDate());
        dto.setCashAccountId(entity.getCashAccount() != null ? entity.getCashAccount().getId() : null);
        dto.setBankAccountId(entity.getBankAccount() != null ? entity.getBankAccount().getId() : null);
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCurrentAccountId() { return currentAccountId; }
    public void setCurrentAccountId(Long currentAccountId) { this.currentAccountId = currentAccountId; }
    public Long getBuildingId() { return buildingId; }
    public void setBuildingId(Long buildingId) { this.buildingId = buildingId; }
    public String getPaymentType() { return paymentType; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDateTime paymentDate) { this.paymentDate = paymentDate; }
    public Long getCashAccountId() { return cashAccountId; }
    public void setCashAccountId(Long cashAccountId) { this.cashAccountId = cashAccountId; }
    public Long getBankAccountId() { return bankAccountId; }
    public void setBankAccountId(Long bankAccountId) { this.bankAccountId = bankAccountId; }
}
