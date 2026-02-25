package com.saraasansor.api.dto;

import java.time.LocalDate;

public class WarningElevatorDto {
    private String identityNo;
    private LocalDate maintenanceEndDate;
    private String status; // EXPIRED or WARNING
    
    public WarningElevatorDto() {
    }
    
    public WarningElevatorDto(String identityNo, LocalDate maintenanceEndDate, String status) {
        this.identityNo = identityNo;
        this.maintenanceEndDate = maintenanceEndDate;
        this.status = status;
    }
    
    public String getIdentityNo() {
        return identityNo;
    }
    
    public void setIdentityNo(String identityNo) {
        this.identityNo = identityNo;
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
}
