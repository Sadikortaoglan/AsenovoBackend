package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotBlank;

public class BrandUpdateRequest {

    @NotBlank(message = "name is required")
    private String name;

    private Boolean active;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
