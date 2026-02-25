package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class CreateMaintenancePlanRequest {
    @NotNull(message = "Elevator ID is required")
    private Long elevatorId;

    @NotNull(message = "Template ID is required")
    private Long templateId;

    @NotNull(message = "Planned date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedDate;

    private Long assignedTechnicianId;
    private Integer dateWindowDays; // default: 7

    public CreateMaintenancePlanRequest() {
    }

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
    }

    public Long getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Long templateId) {
        this.templateId = templateId;
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }

    public Long getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(Long assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public Integer getDateWindowDays() {
        return dateWindowDays;
    }

    public void setDateWindowDays(Integer dateWindowDays) {
        this.dateWindowDays = dateWindowDays;
    }
}
