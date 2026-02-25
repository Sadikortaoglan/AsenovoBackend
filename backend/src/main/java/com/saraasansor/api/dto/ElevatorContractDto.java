package com.saraasansor.api.dto;

import com.saraasansor.api.model.ElevatorContract;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class ElevatorContractDto {
    private Long id;

    @NotNull
    private Long elevatorId;

    private String elevatorName;

    @NotNull
    private LocalDate contractDate;

    private String contractHtml;
    private String filePath;

    public static ElevatorContractDto fromEntity(ElevatorContract entity) {
        ElevatorContractDto dto = new ElevatorContractDto();
        dto.setId(entity.getId());
        dto.setElevatorId(entity.getElevator().getId());
        dto.setElevatorName(entity.getElevator().getBuildingName());
        dto.setContractDate(entity.getContractDate());
        dto.setContractHtml(entity.getContractHtml());
        dto.setFilePath(entity.getFilePath());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getElevatorId() { return elevatorId; }
    public void setElevatorId(Long elevatorId) { this.elevatorId = elevatorId; }
    public String getElevatorName() { return elevatorName; }
    public void setElevatorName(String elevatorName) { this.elevatorName = elevatorName; }
    public LocalDate getContractDate() { return contractDate; }
    public void setContractDate(LocalDate contractDate) { this.contractDate = contractDate; }
    public String getContractHtml() { return contractHtml; }
    public void setContractHtml(String contractHtml) { this.contractHtml = contractHtml; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
