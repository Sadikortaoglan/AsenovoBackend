package com.saraasansor.api.dto;

/**
 * DTO for sidebar badge counts
 */
public class CountsDto {
    private Long elevators;
    private Long maintenances;
    private Long inspections;
    private Long faults;
    private Long parts;
    private Long warnings;
    private Long maintenanceTemplates;
    private Long maintenancePlansUpcoming;
    private Long maintenancePlansCompleted;
    private Long maintenanceSessionsCompleted;
    
    public CountsDto() {
    }
    
    // Getters and Setters
    public Long getElevators() {
        return elevators;
    }
    
    public void setElevators(Long elevators) {
        this.elevators = elevators;
    }
    
    public Long getMaintenances() {
        return maintenances;
    }
    
    public void setMaintenances(Long maintenances) {
        this.maintenances = maintenances;
    }
    
    public Long getInspections() {
        return inspections;
    }
    
    public void setInspections(Long inspections) {
        this.inspections = inspections;
    }
    
    public Long getFaults() {
        return faults;
    }
    
    public void setFaults(Long faults) {
        this.faults = faults;
    }
    
    public Long getParts() {
        return parts;
    }
    
    public void setParts(Long parts) {
        this.parts = parts;
    }
    
    public Long getWarnings() {
        return warnings;
    }
    
    public void setWarnings(Long warnings) {
        this.warnings = warnings;
    }
    
    public Long getMaintenanceTemplates() {
        return maintenanceTemplates;
    }
    
    public void setMaintenanceTemplates(Long maintenanceTemplates) {
        this.maintenanceTemplates = maintenanceTemplates;
    }
    
    public Long getMaintenancePlansUpcoming() {
        return maintenancePlansUpcoming;
    }
    
    public void setMaintenancePlansUpcoming(Long maintenancePlansUpcoming) {
        this.maintenancePlansUpcoming = maintenancePlansUpcoming;
    }
    
    public Long getMaintenancePlansCompleted() {
        return maintenancePlansCompleted;
    }
    
    public void setMaintenancePlansCompleted(Long maintenancePlansCompleted) {
        this.maintenancePlansCompleted = maintenancePlansCompleted;
    }
    
    public Long getMaintenanceSessionsCompleted() {
        return maintenanceSessionsCompleted;
    }
    
    public void setMaintenanceSessionsCompleted(Long maintenanceSessionsCompleted) {
        this.maintenanceSessionsCompleted = maintenanceSessionsCompleted;
    }
}
