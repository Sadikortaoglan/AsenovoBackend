package com.saraasansor.api.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "elevators")
public class Elevator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "identity_number", unique = true, nullable = false)
    private String identityNumber;

    @Column(name = "building_name", nullable = false)
    private String buildingName;

    @Column(nullable = false)
    private String address;

    @Column(name = "elevator_number")
    private String elevatorNumber;
    
    @Column(name = "floor_count")
    private Integer floorCount;
    
    private Integer capacity;
    private Double speed;
    
    @Column(name = "technical_notes")
    private String technicalNotes;

    @Column(name = "drive_type")
    private String driveType;
    
    @Column(name = "machine_brand")
    private String machineBrand;
    
    @Column(name = "door_type")
    private String doorType;
    
    @Column(name = "installation_year")
    private Integer installationYear;
    
    @Column(name = "serial_number")
    private String serialNumber;
    
    @Column(name = "control_system")
    private String controlSystem;
    
    private String rope;
    private String modernization;

    @Column(name = "inspection_date", nullable = false)
    private LocalDate inspectionDate;

    @Column(name = "label_date", nullable = false)
    private LocalDate labelDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "label_type", nullable = false)
    private LabelType labelType;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.ACTIVE;

    @Column(name = "blue_label")
    private Boolean blueLabel;

    @Column(name = "manager_name")
    private String managerName;

    @Column(name = "manager_tc_identity_no", nullable = false, length = 11)
    private String managerTcIdentityNo;

    @Column(name = "manager_phone", nullable = false)
    private String managerPhone;

    @Column(name = "manager_email")
    private String managerEmail;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum Status {
        ACTIVE, EXPIRED
    }

    public Elevator() {
    }

    public Elevator(Long id, String identityNumber, String buildingName, String address, String elevatorNumber, Integer floorCount, Integer capacity, Double speed, String technicalNotes, String driveType, String machineBrand, String doorType, Integer installationYear, String serialNumber, String controlSystem, String rope, String modernization, LocalDate inspectionDate, LocalDate labelDate, LabelType labelType, LocalDate expiryDate, Status status, Boolean blueLabel, String managerName, String managerTcIdentityNo, String managerPhone, String managerEmail, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.identityNumber = identityNumber;
        this.buildingName = buildingName;
        this.address = address;
        this.elevatorNumber = elevatorNumber;
        this.floorCount = floorCount;
        this.capacity = capacity;
        this.speed = speed;
        this.technicalNotes = technicalNotes;
        this.driveType = driveType;
        this.machineBrand = machineBrand;
        this.doorType = doorType;
        this.installationYear = installationYear;
        this.serialNumber = serialNumber;
        this.controlSystem = controlSystem;
        this.rope = rope;
        this.modernization = modernization;
        this.inspectionDate = inspectionDate;
        this.labelDate = labelDate;
        this.labelType = labelType;
        this.expiryDate = expiryDate;
        this.status = status;
        this.blueLabel = blueLabel;
        this.managerName = managerName;
        this.managerTcIdentityNo = managerTcIdentityNo;
        this.managerPhone = managerPhone;
        this.managerEmail = managerEmail;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
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

    public LocalDate getLabelDate() {
        return labelDate;
    }

    public void setLabelDate(LocalDate labelDate) {
        this.labelDate = labelDate;
    }

    public LabelType getLabelType() {
        return labelType;
    }

    public void setLabelType(LabelType labelType) {
        this.labelType = labelType;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
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
        this.managerPhone = managerPhone;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public void setManagerEmail(String managerEmail) {
        this.managerEmail = managerEmail;
    }
}
