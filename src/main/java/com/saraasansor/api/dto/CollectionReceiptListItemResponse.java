package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BUnitTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class CollectionReceiptListItemResponse {

    private Long id;
    private String receiptNumber;
    private LocalDate collectionDate;
    private Long b2bUnitId;
    private String b2bUnitName;
    private Long facilityId;
    private String facilityName;
    private BigDecimal amount;
    private B2BUnitTransaction.TransactionType collectionType;
    private LocalDateTime createdAt;
    private String createdBy;

    public static CollectionReceiptListItemResponse fromEntity(B2BUnitTransaction transaction) {
        CollectionReceiptListItemResponse response = new CollectionReceiptListItemResponse();
        response.setId(transaction.getId());
        response.setReceiptNumber(buildReceiptNumber(transaction.getId()));
        response.setCollectionDate(transaction.getTransactionDate());
        response.setB2bUnitId(transaction.getB2bUnit() != null ? transaction.getB2bUnit().getId() : null);
        response.setB2bUnitName(transaction.getB2bUnit() != null ? transaction.getB2bUnit().getName() : null);
        response.setFacilityId(transaction.getFacility() != null ? transaction.getFacility().getId() : null);
        response.setFacilityName(transaction.getFacility() != null ? transaction.getFacility().getName() : null);
        response.setAmount(transaction.getAmount());
        response.setCollectionType(transaction.getTransactionType());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setCreatedBy(transaction.getCreatedBy());
        return response;
    }

    public static String buildReceiptNumber(Long id) {
        if (id == null) {
            return null;
        }
        return "COL-" + String.format("%08d", id);
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public B2BUnitTransaction.TransactionType getCollectionType() {
        return collectionType;
    }

    public void setCollectionType(B2BUnitTransaction.TransactionType collectionType) {
        this.collectionType = collectionType;
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
