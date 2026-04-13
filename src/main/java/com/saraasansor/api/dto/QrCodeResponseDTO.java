package com.saraasansor.api.dto;

import com.saraasansor.api.model.LabelType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class QrCodeResponseDTO {

    private Long id;
    private UUID uuid;
    private Long elevatorId;
    private String elevatorName;
    private String buildingName;
    private Long facilityId;
    private String facilityName;
    private Long b2bUnitId;
    private String b2bUnitName;
    private String customerName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LabelType labelType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
    private Boolean attachmentExists;
    private String attachmentOriginalFileName;
    private String attachmentContentType;
    private Long attachmentSize;
    private String attachmentUrl;
    private boolean hasQr;
    private String qrPngBase64;
    private String qrImageUrl;
    private String qrPrintUrl;

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

    public Long getFacilityId() {
        return facilityId;
    }

    public void setFacilityId(Long facilityId) {
        this.facilityId = facilityId;
    }

    public String getFacilityName() {
        return facilityName;
    }

    public void setFacilityName(String facilityName) {
        this.facilityName = facilityName;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public Long getB2bUnitId() {
        return b2bUnitId;
    }

    public void setB2bUnitId(Long b2bUnitId) {
        this.b2bUnitId = b2bUnitId;
    }

    public String getB2bUnitName() {
        return b2bUnitName;
    }

    public void setB2bUnitName(String b2bUnitName) {
        this.b2bUnitName = b2bUnitName;
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

    public LabelType getLabelType() {
        return labelType;
    }

    public void setLabelType(LabelType labelType) {
        this.labelType = labelType;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getAttachmentExists() {
        return attachmentExists;
    }

    public void setAttachmentExists(Boolean attachmentExists) {
        this.attachmentExists = attachmentExists;
    }

    public String getAttachmentOriginalFileName() {
        return attachmentOriginalFileName;
    }

    public void setAttachmentOriginalFileName(String attachmentOriginalFileName) {
        this.attachmentOriginalFileName = attachmentOriginalFileName;
    }

    public String getAttachmentContentType() {
        return attachmentContentType;
    }

    public void setAttachmentContentType(String attachmentContentType) {
        this.attachmentContentType = attachmentContentType;
    }

    public Long getAttachmentSize() {
        return attachmentSize;
    }

    public void setAttachmentSize(Long attachmentSize) {
        this.attachmentSize = attachmentSize;
    }

    public String getAttachmentUrl() {
        return attachmentUrl;
    }

    public void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
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

    public String getQrImageUrl() {
        return qrImageUrl;
    }

    public void setQrImageUrl(String qrImageUrl) {
        this.qrImageUrl = qrImageUrl;
    }

    public String getQrPrintUrl() {
        return qrPrintUrl;
    }

    public void setQrPrintUrl(String qrPrintUrl) {
        this.qrPrintUrl = qrPrintUrl;
    }
}
