package com.saraasansor.api.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

public class ElevatorLabelResponse {

    private Long id;
    private Long elevatorId;
    private String elevatorName;
    private Long facilityId;
    private String facilityName;
    private String labelName;
    private LocalDate labelStartDate;
    private LocalDate labelEndDate;
    private LocalDate labelIssueDate;
    private LocalDate labelDate;
    private LocalDate expiryDate;
    private String labelType;
    private String serialNumber;
    private String contractNumber;
    private String description;
    private String status;
    private Long qrCodeId;
    private String attachmentName;
    private String attachmentContentType;
    private Long attachmentSize;
    private String attachmentUrl;
    private boolean attachmentExists;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Map<String, Object> additionalFields = new LinkedHashMap<>();

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

    public String getElevatorName() {
        return elevatorName;
    }

    public void setElevatorName(String elevatorName) {
        this.elevatorName = elevatorName;
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

    public String getLabelName() {
        return labelName;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }

    public LocalDate getLabelStartDate() {
        return labelStartDate;
    }

    public void setLabelStartDate(LocalDate labelStartDate) {
        this.labelStartDate = labelStartDate;
    }

    public LocalDate getLabelEndDate() {
        return labelEndDate;
    }

    public void setLabelEndDate(LocalDate labelEndDate) {
        this.labelEndDate = labelEndDate;
    }

    public LocalDate getLabelIssueDate() {
        return labelIssueDate;
    }

    public void setLabelIssueDate(LocalDate labelIssueDate) {
        this.labelIssueDate = labelIssueDate;
    }

    public LocalDate getLabelDate() {
        return labelDate;
    }

    public void setLabelDate(LocalDate labelDate) {
        this.labelDate = labelDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public String getLabelType() {
        return labelType;
    }

    public void setLabelType(String labelType) {
        this.labelType = labelType;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public String getContractNumber() {
        return contractNumber;
    }

    public void setContractNumber(String contractNumber) {
        this.contractNumber = contractNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(Long qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public String getAttachmentName() {
        return attachmentName;
    }

    public void setAttachmentName(String attachmentName) {
        this.attachmentName = attachmentName;
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

    public boolean isAttachmentExists() {
        return attachmentExists;
    }

    public void setAttachmentExists(boolean attachmentExists) {
        this.attachmentExists = attachmentExists;
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

    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields == null ? new LinkedHashMap<>() : additionalFields;
    }
}
