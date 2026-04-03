package com.saraasansor.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public class QuickCollectionCreateRequest {

    public enum CollectionType {
        CASH,
        PAYTR,
        CREDIT_CARD,
        BANK,
        CHEQUE,
        PROMISSORY_NOTE
    }

    @NotNull(message = "collectionDate is required")
    private LocalDate collectionDate;

    @NotNull(message = "b2bUnitId is required")
    private Long b2bUnitId;

    private Long facilityId;

    @NotNull(message = "collectionType is required")
    private CollectionType collectionType;

    @NotNull(message = "amount is required")
    @DecimalMin(value = "0.01", inclusive = true, message = "amount must be greater than zero")
    private BigDecimal amount;

    private String description;

    private Long cashboxId;
    private Long cashAccountId;
    private Long bankAccountId;
    private Long cardBankId;
    private LocalDate dueDate;
    private String chequeSerialNo;
    private String promissorySerialNo;
    private String paytrReference;

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public Long getB2bUnitId() {
        return b2bUnitId;
    }

    public void setB2bUnitId(Long b2bUnitId) {
        this.b2bUnitId = b2bUnitId;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public CollectionType getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(CollectionType collectionType) {
        this.collectionType = collectionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCashboxId() {
        return cashboxId;
    }

    public void setCashboxId(Long cashboxId) {
        this.cashboxId = cashboxId;
    }

    public Long getCashAccountId() {
        return cashAccountId;
    }

    public void setCashAccountId(Long cashAccountId) {
        this.cashAccountId = cashAccountId;
    }

    public Long getBankAccountId() {
        return bankAccountId;
    }

    public void setBankAccountId(Long bankAccountId) {
        this.bankAccountId = bankAccountId;
    }

    public Long getCardBankId() {
        return cardBankId;
    }

    public void setCardBankId(Long cardBankId) {
        this.cardBankId = cardBankId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getChequeSerialNo() {
        return chequeSerialNo;
    }

    public void setChequeSerialNo(String chequeSerialNo) {
        this.chequeSerialNo = chequeSerialNo;
    }

    public String getPromissorySerialNo() {
        return promissorySerialNo;
    }

    public void setPromissorySerialNo(String promissorySerialNo) {
        this.promissorySerialNo = promissorySerialNo;
    }

    public String getPaytrReference() {
        return paytrReference;
    }

    public void setPaytrReference(String paytrReference) {
        this.paytrReference = paytrReference;
    }
}
