package com.saraasansor.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class B2BUnitReportData {

    private String companyName;
    private String companyLogoUrl;
    private String reportType;
    private LocalDateTime reportCreatedAt;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String b2bUnitName;
    private String b2bUnitAddress;
    private String b2bUnitPhone;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private String balanceLabel;
    private List<B2BUnitReportRow> rows = new ArrayList<>();

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getCompanyLogoUrl() {
        return companyLogoUrl;
    }

    public void setCompanyLogoUrl(String companyLogoUrl) {
        this.companyLogoUrl = companyLogoUrl;
    }

    public String getReportType() {
        return reportType;
    }

    public void setReportType(String reportType) {
        this.reportType = reportType;
    }

    public LocalDateTime getReportCreatedAt() {
        return reportCreatedAt;
    }

    public void setReportCreatedAt(LocalDateTime reportCreatedAt) {
        this.reportCreatedAt = reportCreatedAt;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
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

    public String getB2bUnitPhone() {
        return b2bUnitPhone;
    }

    public void setB2bUnitPhone(String b2bUnitPhone) {
        this.b2bUnitPhone = b2bUnitPhone;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
    }

    public String getBalanceLabel() {
        return balanceLabel;
    }

    public void setBalanceLabel(String balanceLabel) {
        this.balanceLabel = balanceLabel;
    }

    public List<B2BUnitReportRow> getRows() {
        return rows;
    }

    public void setRows(List<B2BUnitReportRow> rows) {
        this.rows = rows;
    }
}
