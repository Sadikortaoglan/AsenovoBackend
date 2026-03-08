package com.saraasansor.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public class QrCodeResponseDTO {

    private Long id;
    private UUID uuid;
    private Long elevatorId;
    private String elevatorName;
    private String buildingName;
    private String customerName;
    private LocalDateTime createdAt;
    private boolean hasQr;
    private String qrPngBase64;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
    }

    public String getElevatorName() {
        return elevatorName;
    }

    public void setElevatorName(String elevatorName) {
        this.elevatorName = elevatorName;
    }

    public String getBuildingName() {
        return buildingName;
    }

    public void setBuildingName(String buildingName) {
        this.buildingName = buildingName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isHasQr() {
        return hasQr;
    }

    public void setHasQr(boolean hasQr) {
        this.hasQr = hasQr;
    }

    public String getQrPngBase64() {
        return qrPngBase64;
    }

    public void setQrPngBase64(String qrPngBase64) {
        this.qrPngBase64 = qrPngBase64;
    }
}
