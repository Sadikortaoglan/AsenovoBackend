package com.saraasansor.api.dto;

import com.saraasansor.api.model.Elevator;

import java.time.LocalDate;

public class WarningDto {
    private String identityNo;
    private String buildingName;
    private String address;
    private LocalDate maintenanceEndDate; // This is the expiryDate from elevator
    private String status; // EXPIRED, WARNING, OK
    
    public WarningDto() {
    }
    
    public WarningDto(String identityNo, String buildingName, String address, LocalDate maintenanceEndDate, String status) {
        this.identityNo = identityNo;
        this.buildingName = buildingName;
        this.address = address;
        this.maintenanceEndDate = maintenanceEndDate;
        this.status = status;
    }
    
    public String getIdentityNo() {
        return identityNo;
    }
    
    public void setIdentityNo(String identityNo) {
        this.identityNo = identityNo;
    }
    
    public String getBuildingName() {
        return buildingName;
    }
    
    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public LocalDate getMaintenanceEndDate() {
        return maintenanceEndDate;
    }
    
    public void setMaintenanceEndDate(LocalDate maintenanceEndDate) {
        this.maintenanceEndDate = maintenanceEndDate;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public static WarningDto fromEntity(Elevator elevator, String status) {
        WarningDto dto = new WarningDto();
        dto.setIdentityNo(elevator.getIdentityNumber() != null ? elevator.getIdentityNumber() : "");
        dto.setBuildingName(elevator.getBuildingName() != null ? elevator.getBuildingName() : "");
        dto.setAddress(elevator.getAddress() != null ? elevator.getAddress() : "");
        dto.setMaintenanceEndDate(elevator.getExpiryDate());
        dto.setStatus(status);
        return dto;
    }
}
