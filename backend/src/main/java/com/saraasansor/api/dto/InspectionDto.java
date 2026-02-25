package com.saraasansor.api.dto;

import com.saraasansor.api.model.Inspection;
import com.saraasansor.api.model.InspectionColor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class InspectionDto {
    private Long id;
    
    @NotNull(message = "Elevator ID cannot be empty")
    private Long elevatorId;
    
    private String elevatorBuildingName;
    private String elevatorIdentityNumber;
    private String elevatorCode; // elevatorNumber or identityNumber
    private String elevatorName; // elevatorNumber or identityNumber (alias for frontend compatibility)
    
    @NotNull(message = "Date cannot be empty")
    private LocalDate date;
    private LocalDate inspectionDate; // Alias for date field (for frontend compatibility)
    
    @NotBlank(message = "Result cannot be empty")
    private String result;
    
    @NotNull(message = "Inspection color is required")
    private String inspectionColor; // GREEN, YELLOW, RED, ORANGE
    
    private String contactedPersonName; // Optional: name of person contacted during inspection
    
    private String description;
    private LocalDateTime createdAt;

    public InspectionDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
    }

    public String getElevatorBuildingName() {
        return elevatorBuildingName;
    }

    public void setElevatorBuildingName(String elevatorBuildingName) {
        this.elevatorBuildingName = elevatorBuildingName;
    }

    public String getElevatorIdentityNumber() {
        return elevatorIdentityNumber;
    }

    public void setElevatorIdentityNumber(String elevatorIdentityNumber) {
        this.elevatorIdentityNumber = elevatorIdentityNumber;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getInspectionColor() {
        return inspectionColor;
    }

    public void setInspectionColor(String inspectionColor) {
        this.inspectionColor = inspectionColor;
    }

    public String getContactedPersonName() {
        return contactedPersonName;
    }

    public void setContactedPersonName(String contactedPersonName) {
        this.contactedPersonName = contactedPersonName;
    }

    public String getElevatorCode() {
        return elevatorCode;
    }

    public void setElevatorCode(String elevatorCode) {
        this.elevatorCode = elevatorCode;
    }

    public String getElevatorName() {
        return elevatorName;
    }

    public void setElevatorName(String elevatorName) {
        this.elevatorName = elevatorName;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDate inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public static InspectionDto fromEntity(Inspection inspection) {
        InspectionDto dto = new InspectionDto();
        dto.setId(inspection.getId());
        
        if (inspection.getElevator() != null) {
            dto.setElevatorId(inspection.getElevator().getId());
            dto.setElevatorBuildingName(inspection.getElevator().getBuildingName());
            dto.setElevatorIdentityNumber(inspection.getElevator().getIdentityNumber());
            // Set elevatorCode and elevatorName (use elevatorNumber if available, otherwise identityNumber)
            String elevatorCode = inspection.getElevator().getElevatorNumber() != null 
                ? inspection.getElevator().getElevatorNumber() 
                : inspection.getElevator().getIdentityNumber();
            dto.setElevatorCode(elevatorCode);
            dto.setElevatorName(elevatorCode);
        }
        
        dto.setDate(inspection.getDate());
        dto.setInspectionDate(inspection.getDate()); // Alias for frontend compatibility
        dto.setResult(inspection.getResult());
        dto.setInspectionColor(inspection.getInspectionColor() != null ? inspection.getInspectionColor().name() : null);
        dto.setContactedPersonName(inspection.getContactedPersonName());
        dto.setDescription(inspection.getDescription());
        dto.setCreatedAt(inspection.getCreatedAt());
        return dto;
    }
}
