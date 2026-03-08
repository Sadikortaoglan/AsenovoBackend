package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CreateB2BUnitGroupRequest {

    @NotBlank(message = "Group name is required")
    private String name;

    private String description;

    public CreateB2BUnitGroupRequest() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
