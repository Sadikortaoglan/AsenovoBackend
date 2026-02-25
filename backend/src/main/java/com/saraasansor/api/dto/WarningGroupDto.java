package com.saraasansor.api.dto;

import java.util.ArrayList;
import java.util.List;

public class WarningGroupDto {
    private String buildingName;
    private String address;
    private String status; // EXPIRED or WARNING
    private List<WarningElevatorDto> elevators = new ArrayList<>();
    
    public WarningGroupDto() {
    }
    
    public WarningGroupDto(String buildingName, String address, String status, List<WarningElevatorDto> elevators) {
        this.buildingName = buildingName;
        this.address = address;
        this.status = status;
        this.elevators = elevators != null ? elevators : new ArrayList<>();
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public List<WarningElevatorDto> getElevators() {
        return elevators;
    }
    
    public void setElevators(List<WarningElevatorDto> elevators) {
        this.elevators = elevators != null ? elevators : new ArrayList<>();
    }
}
