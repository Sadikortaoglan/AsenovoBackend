package com.saraasansor.api.dto;

import com.saraasansor.api.model.MaintenancePlan;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class MaintenancePlanResponseDto {
    private Long id;
    private Long elevatorId;
    private String elevatorCode;
    private String elevatorBuildingName;
    private String elevatorAddress;
    private Long templateId;
    private String templateName;
    private LocalDate plannedDate;
    private Long assignedTechnicianId;
    private String assignedTechnicianName;
    private String status;
    private LocalDateTime completedDate;
    private String qrCode;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MaintenancePlanResponseDto() {
    }

    public static MaintenancePlanResponseDto fromEntity(MaintenancePlan plan) {
        if (plan == null) {
            return null;
        }
        
        MaintenancePlanResponseDto dto = new MaintenancePlanResponseDto();
        dto.setId(plan.getId());
        
        if (plan.getElevator() != null) {
            dto.setElevatorId(plan.getElevator().getId());
            String elevatorCode = plan.getElevator().getElevatorNumber();
            if (elevatorCode == null || elevatorCode.isEmpty()) {
                elevatorCode = plan.getElevator().getIdentityNumber();
            }
            dto.setElevatorCode(elevatorCode != null ? elevatorCode : "");
            dto.setElevatorBuildingName(plan.getElevator().getBuildingName() != null ? 
                plan.getElevator().getBuildingName() : "");
            dto.setElevatorAddress(plan.getElevator().getAddress() != null ? 
                plan.getElevator().getAddress() : "");
        }
        
        if (plan.getTemplate() != null) {
            dto.setTemplateId(plan.getTemplate().getId());
            dto.setTemplateName(plan.getTemplate().getName() != null ? 
                plan.getTemplate().getName() : "");
        }
        
        dto.setPlannedDate(plan.getPlannedDate());
        
        if (plan.getAssignedTechnician() != null) {
            dto.setAssignedTechnicianId(plan.getAssignedTechnician().getId());
            dto.setAssignedTechnicianName(plan.getAssignedTechnician().getUsername() != null ? 
                plan.getAssignedTechnician().getUsername() : "");
        }
        
        dto.setStatus(plan.getStatus() != null ? plan.getStatus().name() : "PLANNED");
        dto.setNote(plan.getNote());
        dto.setCreatedAt(plan.getCreatedAt());
        dto.setUpdatedAt(plan.getUpdatedAt() != null ? plan.getUpdatedAt() : plan.getCreatedAt());
        
        if (plan.getCompletedAt() != null) {
            dto.setCompletedDate(plan.getCompletedAt());
        }
        
        return dto;
    }

    // Getters and Setters
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

    public String getElevatorCode() {
        return elevatorCode;
    }

    public void setElevatorCode(String elevatorCode) {
        this.elevatorCode = elevatorCode;
    }

    public String getElevatorBuildingName() {
        return elevatorBuildingName;
    }

    public void setElevatorBuildingName(String elevatorBuildingName) {
        this.elevatorBuildingName = elevatorBuildingName;
    }

    public String getElevatorAddress() {
        return elevatorAddress;
    }

    public void setElevatorAddress(String elevatorAddress) {
        this.elevatorAddress = elevatorAddress;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public Long getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(Long assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getAssignedTechnicianName() {
        return assignedTechnicianName;
    }

    public void setAssignedTechnicianName(String assignedTechnicianName) {
        this.assignedTechnicianName = assignedTechnicianName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCompletedDate() {
        return completedDate;
    }

    public void setCompletedDate(LocalDateTime completedDate) {
        this.completedDate = completedDate;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
