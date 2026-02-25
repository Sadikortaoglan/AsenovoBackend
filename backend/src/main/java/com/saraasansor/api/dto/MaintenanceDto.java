package com.saraasansor.api.dto;

import com.saraasansor.api.model.Maintenance;

import java.time.LocalDate;

public class MaintenanceDto {
    private Long id;
    private Long elevatorId;
    private String elevatorBuildingName;
    private LocalDate date;
    private String labelType; // GREEN, BLUE, YELLOW, RED
    private String description;
    // technicianUserId and technicianUsername are READ-ONLY (response only)
    // They are NOT accepted in request body - technician is auto-assigned from SecurityContext
    private Long technicianUserId;
    private String technicianUsername;
    private Double amount;
    private Boolean isPaid;
    private LocalDate paymentDate;
    
    /**
     * QR token for maintenance creation validation
     * Required for TECHNICIAN (PERSONEL) role
     * Optional for ADMIN role (can omit for remote start)
     * Format: "e={elevatorCode}&s={signature}" or full URL
     */
    private String qrToken;
    
    public MaintenanceDto() {
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTechnicianUserId() {
        return technicianUserId;
    }

    public void setTechnicianUserId(Long technicianUserId) {
        this.technicianUserId = technicianUserId;
    }

    public String getTechnicianUsername() {
        return technicianUsername;
    }

    public void setTechnicianUsername(String technicianUsername) {
        this.technicianUsername = technicianUsername;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public Boolean getIsPaid() {
        return isPaid;
    }

    public void setIsPaid(Boolean isPaid) {
        this.isPaid = isPaid;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }
    
    public String getQrToken() {
        return qrToken;
    }
    
    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }

    public String getLabelType() {
        return labelType;
    }

    public void setLabelType(String labelType) {
        this.labelType = labelType;
    }

    public static MaintenanceDto fromEntity(Maintenance maintenance) {
        MaintenanceDto dto = new MaintenanceDto();
        dto.setId(maintenance.getId());
        dto.setElevatorId(maintenance.getElevator().getId());
        dto.setElevatorBuildingName(maintenance.getElevator().getBuildingName());
        dto.setDate(maintenance.getDate());
        dto.setLabelType(maintenance.getLabelType() != null ? maintenance.getLabelType().name() : null);
        dto.setDescription(maintenance.getDescription());
        if (maintenance.getTechnician() != null) {
            dto.setTechnicianUserId(maintenance.getTechnician().getId());
            dto.setTechnicianUsername(maintenance.getTechnician().getUsername());
        }
        dto.setAmount(maintenance.getAmount());
        dto.setIsPaid(maintenance.getIsPaid());
        dto.setPaymentDate(maintenance.getPaymentDate());
        return dto;
    }
}
