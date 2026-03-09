package com.saraasansor.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class B2BUnitReportRow {

    private LocalDate transactionDate;
    private String transactionTypeLabel;
    private BigDecimal debitAmount;
    private BigDecimal creditAmount;
    private BigDecimal balanceAfterTransaction;

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getTransactionTypeLabel() {
        return transactionTypeLabel;
    }

    public void setTransactionTypeLabel(String transactionTypeLabel) {
        this.transactionTypeLabel = transactionTypeLabel;
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
}
