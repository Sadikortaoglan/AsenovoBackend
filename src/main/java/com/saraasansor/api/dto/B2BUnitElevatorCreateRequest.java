package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class B2BUnitElevatorCreateRequest {

    @NotNull(message = "facilityId is required")
    private Long facilityId;

    @NotBlank(message = "Identity number is required")
    private String identityNumber;

    @NotBlank(message = "Name is required")
    private String name;

    private String maintenanceType;
    private String elevatorType;
    private String doorType;
    private String hazardType;
    private String brand;

    @Min(value = 0, message = "constructionYear must be zero or positive")
    private Integer constructionYear;

    @Min(value = 0, message = "stopCount must be zero or positive")
    private Integer stopCount;

    @Min(value = 0, message = "capacity must be zero or positive")
    private Integer capacity;

    @DecimalMin(value = "0.0", inclusive = true, message = "speed must be zero or positive")
    private Double speed;

    private String warrantyStatus;
    private LocalDate warrantyEndDate;
    private Long maintenanceStaffId;
    private Long failureStaffId;
    private String addressText;
    private String description;

    @DecimalMin(value = "-90.0", inclusive = true, message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0", inclusive = true, message = "Latitude must be <= 90")
    private Double mapLat;

    @DecimalMin(value = "-180.0", inclusive = true, message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0", inclusive = true, message = "Longitude must be <= 180")
    private Double mapLng;

    @NotNull(message = "Label date is required")
    private LocalDate labelDate;

    @NotBlank(message = "Label type is required")
    private String labelType;

    @NotNull(message = "End date is required")
    @JsonAlias("endDate")
    private LocalDate expiryDate;

    @NotBlank(message = "Manager name is required")
    private String managerName;

    @NotBlank(message = "Manager TC Identity Number is required")
    @Pattern(regexp = "^[0-9]{11}$", message = "Manager TC Identity Number must be exactly 11 digits")
    private String managerTcIdentityNo;

    @NotBlank(message = "Manager phone number is required")
    @Pattern(regexp = "^(0?[0-9]{10})$", message = "Phone number must be 10 or 11 digits (Turkish format, digits only)")
    private String managerPhone;

    private String managerEmail;

    public B2BUnitElevatorCreateRequest() {
    }

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getIdentityNumber() {
        return identityNumber;
    }

    public void setIdentityNumber(String identityNumber) {
        this.identityNumber = identityNumber;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMaintenanceType() {
        return maintenanceType;
    }

    public void setMaintenanceType(String maintenanceType) {
        this.maintenanceType = maintenanceType;
    }

    public String getElevatorType() {
        return elevatorType;
    }

    public void setElevatorType(String elevatorType) {
        this.elevatorType = elevatorType;
    }

    public String getDoorType() {
        return doorType;
    }

    public void setDoorType(String doorType) {
        this.doorType = doorType;
    }

    public String getHazardType() {
        return hazardType;
    }

    public void setHazardType(String hazardType) {
        this.hazardType = hazardType;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public Integer getConstructionYear() {
        return constructionYear;
    }

    public void setConstructionYear(Integer constructionYear) {
        this.constructionYear = constructionYear;
    }

    public Integer getStopCount() {
        return stopCount;
    }

    public void setStopCount(Integer stopCount) {
        this.stopCount = stopCount;
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

    public String getWarrantyStatus() {
        return warrantyStatus;
    }

    public void setWarrantyStatus(String warrantyStatus) {
        this.warrantyStatus = warrantyStatus;
    }

    public LocalDate getWarrantyEndDate() {
        return warrantyEndDate;
    }

    public void setWarrantyEndDate(LocalDate warrantyEndDate) {
        this.warrantyEndDate = warrantyEndDate;
    }

    public Long getMaintenanceStaffId() {
        return maintenanceStaffId;
    }

    public void setMaintenanceStaffId(Long maintenanceStaffId) {
        this.maintenanceStaffId = maintenanceStaffId;
    }

    public Long getFailureStaffId() {
        return failureStaffId;
    }

    public void setFailureStaffId(Long failureStaffId) {
        this.failureStaffId = failureStaffId;
    }

    public String getAddressText() {
        return addressText;
    }

    public void setAddressText(String addressText) {
        this.addressText = addressText;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getMapLat() {
        return mapLat;
    }

    public void setMapLat(Double mapLat) {
        this.mapLat = mapLat;
    }

    public Double getMapLng() {
        return mapLng;
    }

    public void setMapLng(Double mapLng) {
        this.mapLng = mapLng;
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

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
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
