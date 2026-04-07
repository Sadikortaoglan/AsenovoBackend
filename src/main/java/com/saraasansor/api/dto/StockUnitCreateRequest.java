package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotBlank;

public class StockUnitCreateRequest {

    @NotBlank(message = "name is required")
    private String name;

    @NotBlank(message = "abbreviation is required")
    private String abbreviation;

    private Boolean active = true;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(String abbreviation) {
        this.abbreviation = abbreviation;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
