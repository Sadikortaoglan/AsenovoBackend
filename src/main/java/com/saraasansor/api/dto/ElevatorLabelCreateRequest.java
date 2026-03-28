package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

public class ElevatorLabelCreateRequest {

    @NotNull
    private Long elevatorId;
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
    @JsonIgnore
    private MultipartFile payload;
    @JsonIgnore
    private MultipartFile file;
    private Map<String, Object> additionalFields = new LinkedHashMap<>();

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
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

    public void setStartAt(String startAt) {
        this.labelStartDate = parseToLocalDate(startAt);
    }

    public void setStartDate(String startDate) {
        this.labelStartDate = parseToLocalDate(startDate);
    }

    public LocalDate getLabelEndDate() {
        return labelEndDate;
    }

    public void setLabelEndDate(LocalDate labelEndDate) {
        this.labelEndDate = labelEndDate;
    }

    public void setEndAt(String endAt) {
        this.labelEndDate = parseToLocalDate(endAt);
    }

    public void setEndDate(String endDate) {
        this.labelEndDate = parseToLocalDate(endDate);
    }

    public LocalDate getLabelIssueDate() {
        return labelIssueDate;
    }

    public void setLabelIssueDate(LocalDate labelIssueDate) {
        this.labelIssueDate = labelIssueDate;
    }

    public void setIssueDate(String issueDate) {
        this.labelIssueDate = parseToLocalDate(issueDate);
    }

    public void setIssueAt(String issueAt) {
        this.labelIssueDate = parseToLocalDate(issueAt);
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

    public void setExpiryAt(String expiryAt) {
        this.expiryDate = parseToLocalDate(expiryAt);
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

    public MultipartFile getPayload() {
        return payload;
    }

    public void setPayload(MultipartFile payload) {
        this.payload = payload;
    }

    public MultipartFile getFile() {
        return file;
    }

    public void setFile(MultipartFile file) {
        this.file = file;
    }

    public Map<String, Object> getAdditionalFields() {
        return additionalFields;
    }

    public void setAdditionalFields(Map<String, Object> additionalFields) {
        this.additionalFields = additionalFields == null ? new LinkedHashMap<>() : additionalFields;
    }

    @JsonAnySetter
    public void addAdditionalField(String key, Object value) {
        if (additionalFields == null) {
            additionalFields = new LinkedHashMap<>();
        }
        additionalFields.put(key, value);
    }

    private LocalDate parseToLocalDate(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim();
        try {
            return LocalDate.parse(normalized);
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(normalized).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm")).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")).toLocalDate();
        } catch (Exception ignored) {
        }
        try {
            return LocalDate.parse(normalized, DateTimeFormatter.ofPattern("dd.MM.yyyy"));
        } catch (Exception ignored) {
        }
        try {
            return LocalDateTime.parse(normalized, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")).toLocalDate();
        } catch (Exception ignored) {
        }
        int tIndex = normalized.indexOf('T');
        if (tIndex > 0) {
            try {
                return LocalDate.parse(normalized.substring(0, tIndex));
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
