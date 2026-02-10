package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.saraasansor.api.model.Elevator;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ElevatorDto {
    private Long id;
    
    @NotBlank(message = "Identity number is required")
    private String identityNumber;
    
    @NotBlank(message = "Building name is required")
    private String buildingName;
    
    @NotBlank(message = "Address is required")
    private String address;
    private String elevatorNumber;
    private Integer floorCount;
    private Integer capacity;
    private Double speed;
    private String technicalNotes;
    private String driveType;
    private String machineBrand;
    private String doorType;
    private Integer installationYear;
    private String serialNumber;
    private String controlSystem;
    private String rope;
    private String modernization;
    private LocalDate inspectionDate;
    
    @NotNull(message = "Label date is required")
    private LocalDate labelDate; // Generic label date (not blue-specific)
    
    @NotBlank(message = "Label type is required")
    private String labelType; // GREEN, YELLOW, RED, ORANGE, BLUE (BLUE kept for backward compatibility)
    
    // End date (MANDATORY - must be provided by frontend)
    // Accepts both "expiryDate" and "endDate" from frontend
    @NotNull(message = "End date is required")
    @JsonAlias("endDate")
    private LocalDate expiryDate;
    
    private String status; // ACTIVE, EXPIRED
    private Boolean blueLabel; // Deprecated - kept for backward compatibility
    
    // Manager fields
    private String managerName;
    
    @NotBlank(message = "Manager TC Identity Number is required")
    @Pattern(regexp = "^[0-9]{11}$", message = "TC Identity Number must be exactly 11 digits")
    private String managerTcIdentityNo;
    
    @NotBlank(message = "Manager phone number is required")
    @Pattern(regexp = "^(0?[0-9]{10})$", message = "Phone number must be 10 or 11 digits (Turkish format, digits only)")
    private String managerPhone;
    
    private String managerEmail;

    public ElevatorDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getElevatorNumber() {
        return elevatorNumber;
    }

    public void setElevatorNumber(String elevatorNumber) {
        this.elevatorNumber = elevatorNumber;
    }

    public Integer getFloorCount() {
        return floorCount;
    }

    public void setFloorCount(Integer floorCount) {
        this.floorCount = floorCount;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Double getSpeed() {
        return speed;
    }

    public void setSpeed(Double speed) {
        this.speed = speed;
    }

    public String getTechnicalNotes() {
        return technicalNotes;
    }

    public void setTechnicalNotes(String technicalNotes) {
        this.technicalNotes = technicalNotes;
    }

    public String getDriveType() {
        return driveType;
    }

    public void setDriveType(String driveType) {
        this.driveType = driveType;
    }

    public String getMachineBrand() {
        return machineBrand;
    }

    public void setMachineBrand(String machineBrand) {
        this.machineBrand = machineBrand;
    }

    public String getDoorType() {
        return doorType;
    }

    public void setDoorType(String doorType) {
        this.doorType = doorType;
    }

    public Integer getInstallationYear() {
        return installationYear;
    }

    public void setInstallationYear(Integer installationYear) {
        this.installationYear = installationYear;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getControlSystem() {
        return controlSystem;
    }

    public void setControlSystem(String controlSystem) {
        this.controlSystem = controlSystem;
    }

    public String getRope() {
        return rope;
    }

    public void setRope(String rope) {
        this.rope = rope;
    }

    public String getModernization() {
        return modernization;
    }

    public void setModernization(String modernization) {
        this.modernization = modernization;
    }

    public LocalDate getInspectionDate() {
        return inspectionDate;
    }

    public void setInspectionDate(LocalDate inspectionDate) {
        this.inspectionDate = inspectionDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Boolean getBlueLabel() {
        return blueLabel;
    }

    public void setBlueLabel(Boolean blueLabel) {
        this.blueLabel = blueLabel;
    }

    public LocalDate getLabelDate() {
        return labelDate;
    }

    public void setLabelDate(LocalDate labelDate) {
        this.labelDate = labelDate;
    }

    public String getLabelType() {
        return labelType;
    }

    public void setLabelType(String labelType) {
        this.labelType = labelType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getManagerTcIdentityNo() {
        return managerTcIdentityNo;
    }

    public void setManagerTcIdentityNo(String managerTcIdentityNo) {
        this.managerTcIdentityNo = managerTcIdentityNo;
    }

    public String getManagerPhone() {
        return managerPhone;
    }

    public void setManagerPhone(String managerPhone) {
        if (managerPhone != null) {
            // Normalize phone number: remove spaces, dashes, parentheses, and leading +90
            String normalized = managerPhone.trim()
                    .replaceAll("\\s+", "")  // Remove all whitespace
                    .replaceAll("-", "")     // Remove dashes
                    .replaceAll("\\(", "")   // Remove opening parentheses
                    .replaceAll("\\)", "")   // Remove closing parentheses
                    .replaceAll("\\+90", "") // Remove +90 prefix
                    .replaceAll("^90", "");  // Remove 90 prefix (if at start)
            
            // Temporary debug logging
            if (!normalized.equals(managerPhone)) {
                org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ElevatorDto.class);
                log.debug("Phone number normalized: '{}' -> '{}'", managerPhone, normalized);
            }
            
            this.managerPhone = normalized;
        } else {
            this.managerPhone = null;
        }
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
        this.managerEmail = managerEmail;
    }

    public static ElevatorDto fromEntity(Elevator elevator) {
        ElevatorDto dto = new ElevatorDto();
        dto.setId(elevator.getId());
        dto.setIdentityNumber(elevator.getIdentityNumber());
        dto.setBuildingName(elevator.getBuildingName());
        dto.setAddress(elevator.getAddress());
        dto.setElevatorNumber(elevator.getElevatorNumber());
        dto.setFloorCount(elevator.getFloorCount());
        dto.setCapacity(elevator.getCapacity());
        dto.setSpeed(elevator.getSpeed());
        dto.setTechnicalNotes(elevator.getTechnicalNotes());
        dto.setDriveType(elevator.getDriveType());
        dto.setMachineBrand(elevator.getMachineBrand());
        dto.setDoorType(elevator.getDoorType());
        dto.setInstallationYear(elevator.getInstallationYear());
        dto.setSerialNumber(elevator.getSerialNumber());
        dto.setControlSystem(elevator.getControlSystem());
        dto.setRope(elevator.getRope());
        dto.setModernization(elevator.getModernization());
        dto.setInspectionDate(elevator.getInspectionDate());
        dto.setLabelDate(elevator.getLabelDate());
        dto.setLabelType(elevator.getLabelType() != null ? elevator.getLabelType().name() : null);
        dto.setExpiryDate(elevator.getExpiryDate());
        dto.setStatus(elevator.getStatus() != null ? elevator.getStatus().name() : null);
        dto.setBlueLabel(elevator.getBlueLabel());
        dto.setManagerName(elevator.getManagerName());
        dto.setManagerTcIdentityNo(elevator.getManagerTcIdentityNo());
        dto.setManagerPhone(elevator.getManagerPhone());
        dto.setManagerEmail(elevator.getManagerEmail());
        return dto;
    }
}
