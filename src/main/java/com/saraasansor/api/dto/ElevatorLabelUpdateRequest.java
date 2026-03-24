package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotNull;

public class ElevatorLabelUpdateRequest {

    @NotNull
    private Long elevatorId;

    public Long getElevatorId() {
        return elevatorId;
    }

    public void setElevatorId(Long elevatorId) {
        this.elevatorId = elevatorId;
    }
}
