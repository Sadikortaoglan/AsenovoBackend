package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BUnitTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CollectionReceiptPrintResponse {

    private Long id;
    private String receiptNumber;
    private LocalDate collectionDate;
    private B2BUnitTransaction.TransactionType collectionType;
    private BigDecimal amount;
    private String description;

    private Long b2bUnitId;
    private String b2bUnitName;
    private String b2bUnitAddress;

    private Long facilityId;
    private String facilityName;
    private String facilityAddress;

    private Long cashAccountId;
    private Long bankAccountId;
    private LocalDate dueDate;
    private String serialNumber;
    private B2BUnitTransaction.PaymentProvider paymentProvider;
    private BigDecimal balanceAfterTransaction;
    private LocalDateTime createdAt;
    private String createdBy;

    public static CollectionReceiptPrintResponse fromEntity(B2BUnitTransaction transaction) {
        CollectionReceiptPrintResponse response = new CollectionReceiptPrintResponse();
        response.setId(transaction.getId());
        response.setReceiptNumber(CollectionReceiptListItemResponse.buildReceiptNumber(transaction.getId()));
        response.setCollectionDate(transaction.getTransactionDate());
        response.setCollectionType(transaction.getTransactionType());
        response.setAmount(transaction.getAmount());
        response.setDescription(transaction.getDescription());
        response.setB2bUnitId(transaction.getB2bUnit() != null ? transaction.getB2bUnit().getId() : null);
        response.setB2bUnitName(transaction.getB2bUnit() != null ? transaction.getB2bUnit().getName() : null);
        response.setB2bUnitAddress(transaction.getB2bUnit() != null ? transaction.getB2bUnit().getAddress() : null);
        response.setFacilityId(transaction.getFacility() != null ? transaction.getFacility().getId() : null);
        response.setFacilityName(transaction.getFacility() != null ? transaction.getFacility().getName() : null);
        response.setFacilityAddress(transaction.getFacility() != null ? transaction.getFacility().getAddressText() : null);
        response.setCashAccountId(transaction.getCashAccountId());
        response.setBankAccountId(transaction.getBankAccountId());
        response.setDueDate(transaction.getDueDate());
        response.setSerialNumber(transaction.getSerialNumber());
        response.setPaymentProvider(transaction.getPaymentProvider());
        response.setBalanceAfterTransaction(transaction.getBalanceAfterTransaction());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCreatedBy(transaction.getCreatedBy());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getReceiptNumber() {
        return receiptNumber;
    }

    public void setReceiptNumber(String receiptNumber) {
        this.receiptNumber = receiptNumber;
    }

    public LocalDate getCollectionDate() {
        return collectionDate;
    }

    public void setCollectionDate(LocalDate collectionDate) {
        this.collectionDate = collectionDate;
    }

    public B2BUnitTransaction.TransactionType getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(B2BUnitTransaction.TransactionType collectionType) {
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

    public Long getB2bUnitId() {
        return b2bUnitId;
    }

    public void setB2bUnitId(Long b2bUnitId) {
        this.b2bUnitId = b2bUnitId;
    }

    public String getB2bUnitName() {
        return b2bUnitName;
    }

    public void setB2bUnitName(String b2bUnitName) {
        this.b2bUnitName = b2bUnitName;
    }

    public String getB2bUnitAddress() {
        return b2bUnitAddress;
    }

    public void setB2bUnitAddress(String b2bUnitAddress) {
        this.b2bUnitAddress = b2bUnitAddress;
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getFacilityAddress() {
        return facilityAddress;
    }

    public void setFacilityAddress(String facilityAddress) {
        this.facilityAddress = facilityAddress;
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

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public B2BUnitTransaction.PaymentProvider getPaymentProvider() {
        return paymentProvider;
    }

    public void setPaymentProvider(B2BUnitTransaction.PaymentProvider paymentProvider) {
        this.paymentProvider = paymentProvider;
    }

    public BigDecimal getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(BigDecimal balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
