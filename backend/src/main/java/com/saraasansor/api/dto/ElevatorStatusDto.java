package com.saraasansor.api.dto;

import java.time.LocalDate;

public class ElevatorStatusDto {
    private String status; // EXPIRED, WARNING, OK
    private Long daysLeft;
    private String identityNumber;
    private String buildingName;
    private LocalDate expiryDate;

    public ElevatorStatusDto() {
    }

    public ElevatorStatusDto(String status, Long daysLeft, String identityNumber, String buildingName, LocalDate expiryDate) {
        this.status = status;
        this.daysLeft = daysLeft;
        this.identityNumber = identityNumber;
        this.buildingName = buildingName;
        this.expiryDate = expiryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDaysLeft() {
        return daysLeft;
    }

    public void setDaysLeft(Long daysLeft) {
        this.daysLeft = daysLeft;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
