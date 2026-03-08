package com.saraasansor.api.dto;

import com.saraasansor.api.model.B2BUnitTransaction;

import java.math.BigDecimal;
import java.time.LocalDate;

public class B2BUnitTransactionResponse {

    private LocalDate transactionDate;
    private B2BUnitTransaction.TransactionType transactionType;
    private BigDecimal debit;
    private BigDecimal credit;
    private BigDecimal balance;
    private String description;

    public B2BUnitTransactionResponse() {
    }

    public static B2BUnitTransactionResponse fromEntity(B2BUnitTransaction transaction) {
        B2BUnitTransactionResponse response = new B2BUnitTransactionResponse();
        response.setTransactionDate(transaction.getTransactionDate());
        response.setTransactionType(transaction.getTransactionType());
        response.setDebit(transaction.getDebitAmount());
        response.setCredit(transaction.getCreditAmount());
        response.setBalance(transaction.getBalanceAfterTransaction());
        response.setDescription(transaction.getDescription());
        return response;
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
}
