package com.saraasansor.api.dto;

import com.saraasansor.api.model.ElevatorLabel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class ElevatorLabelDto {
    private Long id;

    @NotNull
    private Long elevatorId;

    private String elevatorName;

    @NotBlank
    private String labelName;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    private String description;
    private String filePath;

    public static ElevatorLabelDto fromEntity(ElevatorLabel entity) {
        ElevatorLabelDto dto = new ElevatorLabelDto();
        dto.setId(entity.getId());
        dto.setElevatorId(entity.getElevator().getId());
        dto.setElevatorName(entity.getElevator().getBuildingName());
        dto.setLabelName(entity.getLabelName());
        dto.setStartAt(entity.getStartAt());
        dto.setEndAt(entity.getEndAt());
        dto.setDescription(entity.getDescription());
        dto.setFilePath(entity.getFilePath());
        return dto;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getElevatorId() { return elevatorId; }
    public void setElevatorId(Long elevatorId) { this.elevatorId = elevatorId; }
    public String getElevatorName() { return elevatorName; }
    public void setElevatorName(String elevatorName) { this.elevatorName = elevatorName; }
    public String getLabelName() { return labelName; }
    public void setLabelName(String labelName) { this.labelName = labelName; }
    public LocalDateTime getStartAt() { return startAt; }
    public void setStartAt(LocalDateTime startAt) { this.startAt = startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public void setEndAt(LocalDateTime endAt) { this.endAt = endAt; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
}
