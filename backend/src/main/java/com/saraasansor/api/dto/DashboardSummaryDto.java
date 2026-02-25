package com.saraasansor.api.dto;

public class DashboardSummaryDto {
    private Long totalElevators;
    private Long totalMaintenances;
    private Double totalIncome;
    private Double totalDebt;
    private Long expiredCount;
    private Long warningCount;

    public DashboardSummaryDto() {
    }

    public DashboardSummaryDto(Long totalElevators, Long totalMaintenances, Double totalIncome, Double totalDebt, Long expiredCount, Long warningCount) {
        this.totalElevators = totalElevators;
        this.totalMaintenances = totalMaintenances;
        this.totalIncome = totalIncome;
        this.totalDebt = totalDebt;
        this.expiredCount = expiredCount;
        this.warningCount = warningCount;
    }

    public Long getTotalElevators() {
        return totalElevators;
    }

    public void setTotalElevators(Long totalElevators) {
        this.totalElevators = totalElevators;
    }

    public Long getTotalMaintenances() {
        return totalMaintenances;
    }

    public void setTotalMaintenances(Long totalMaintenances) {
        this.totalMaintenances = totalMaintenances;
    }

    public Double getTotalIncome() {
        return totalIncome;
    }

    public void setTotalIncome(Double totalIncome) {
        this.totalIncome = totalIncome;
    }

    public Double getTotalDebt() {
        return totalDebt;
    }

    public void setTotalDebt(Double totalDebt) {
        this.totalDebt = totalDebt;
    }

    public Long getExpiredCount() {
        return expiredCount;
    }

    public void setExpiredCount(Long expiredCount) {
        this.expiredCount = expiredCount;
    }

    public Long getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(Long warningCount) {
        this.warningCount = warningCount;
    }
}
