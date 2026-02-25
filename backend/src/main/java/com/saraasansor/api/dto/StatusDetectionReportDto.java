package com.saraasansor.api.dto;

import com.saraasansor.api.model.StatusDetectionReport;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class StatusDetectionReportDto {
    private Long id;

    @NotNull
    private LocalDate reportDate;

    @NotBlank
    private String buildingName;

    @NotBlank
    private String elevatorName;

    private String identityNumber;
    private String status;
    private String filePath;
    private String note;

    public static StatusDetectionReportDto fromEntity(StatusDetectionReport entity) {
        StatusDetectionReportDto dto = new StatusDetectionReportDto();
        dto.setId(entity.getId());
        dto.setReportDate(entity.getReportDate());
        dto.setBuildingName(entity.getBuildingName());
        dto.setElevatorName(entity.getElevatorName());
        dto.setIdentityNumber(entity.getIdentityNumber());
        dto.setStatus(entity.getStatus());
        dto.setFilePath(entity.getFilePath());
        dto.setNote(entity.getNote());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public LocalDate getReportDate() { return reportDate; }
    public void setReportDate(LocalDate reportDate) { this.reportDate = reportDate; }
    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }
    public String getElevatorName() { return elevatorName; }
    public void setElevatorName(String elevatorName) { this.elevatorName = elevatorName; }
    public String getIdentityNumber() { return identityNumber; }
    public void setIdentityNumber(String identityNumber) { this.identityNumber = identityNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
}
