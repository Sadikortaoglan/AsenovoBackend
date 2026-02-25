package com.saraasansor.api.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class RescheduleMaintenancePlanRequest {
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate plannedDate;

    public RescheduleMaintenancePlanRequest() {
    }

    public LocalDate getPlannedDate() {
        return plannedDate;
    }

    public void setPlannedDate(LocalDate plannedDate) {
        this.plannedDate = plannedDate;
    }
}
