package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BUnitTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class B2BUnitTransactionResponse {

    private Long id;
    private Long b2bUnitId;
    private Long facilityId;
    private LocalDate transactionDate;
    private B2BUnitTransaction.TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal balanceAfterTransaction;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    private String description;
    private Long cashAccountId;
    private Long bankAccountId;
    private LocalDate dueDate;
    private String serialNumber;
    private B2BUnitTransaction.PaymentProvider paymentProvider;
    private B2BUnitTransaction.TransactionStatus status;
    private LocalDateTime createdAt;

    public B2BUnitTransactionResponse() {
    }

    public static B2BUnitTransactionResponse fromEntity(B2BUnitTransaction transaction) {
        B2BUnitTransactionResponse response = new B2BUnitTransactionResponse();
        response.setId(transaction.getId());
        response.setB2bUnitId(transaction.getB2bUnit() != null ? transaction.getB2bUnit().getId() : null);
        response.setFacilityId(transaction.getFacility() != null ? transaction.getFacility().getId() : null);
        response.setTransactionDate(transaction.getTransactionDate());
        response.setTransactionType(transaction.getTransactionType());
        response.setAmount(transaction.getAmount());
        response.setDebitAmount(transaction.getDebitAmount());
        response.setCreditAmount(transaction.getCreditAmount());
        response.setBalanceAfterTransaction(transaction.getBalanceAfterTransaction());
        response.setDebit(transaction.getDebitAmount());
        response.setCredit(transaction.getCreditAmount());
        response.setBalance(transaction.getBalanceAfterTransaction());
        response.setDescription(transaction.getDescription());
        response.setCashAccountId(transaction.getCashAccountId());
        response.setBankAccountId(transaction.getBankAccountId());
        response.setDueDate(transaction.getDueDate());
        response.setSerialNumber(transaction.getSerialNumber());
        response.setPaymentProvider(transaction.getPaymentProvider());
        response.setStatus(transaction.getStatus());
        response.setCreatedAt(transaction.getCreatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public B2BUnitTransaction.TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(B2BUnitTransaction.TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public BigDecimal getBalanceAfterTransaction() {
        return balanceAfterTransaction;
    }

    public void setBalanceAfterTransaction(BigDecimal balanceAfterTransaction) {
        this.balanceAfterTransaction = balanceAfterTransaction;
    }

    public BigDecimal getDebit() {
        return debit;
    }

    public void setDebit(BigDecimal debit) {
        this.debit = debit;
    }

    public BigDecimal getCredit() {
        return credit;
    }

    public void setCredit(BigDecimal credit) {
        this.credit = credit;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public B2BUnitTransaction.TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(B2BUnitTransaction.TransactionStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
