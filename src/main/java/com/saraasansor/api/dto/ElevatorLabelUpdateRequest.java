package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.saraasansor.api.model.LabelType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public class ElevatorLabelUpdateRequest {

    @NotNull(message = "elevatorId is required")
    @JsonAlias({"elevator_id", "selectedElevatorId", "selected_elevator_id"})
    private Long elevatorId;

    @JsonAlias({"type", "status", "color", "labelStatus"})
    private LabelType labelType;

    @JsonAlias({"labelDate", "start_date", "date"})
    private LocalDate startDate;

    @JsonAlias({"expiryDate", "end_date", "expirationDate"})
    private LocalDate endDate;

    @Size(max = 5000, message = "description must be at most 5000 characters")
    @JsonAlias({"desc", "aciklama", "notes"})
    private String description;

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
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
}
