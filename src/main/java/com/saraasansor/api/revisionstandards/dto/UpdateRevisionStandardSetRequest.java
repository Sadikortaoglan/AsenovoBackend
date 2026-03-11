package com.saraasansor.api.revisionstandards.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateRevisionStandardSetRequest {

    @NotBlank
    private String standardCode;

    public String getStandardCode() {
        return standardCode;
    }

    public void setStandardCode(String standardCode) {
        this.standardCode = standardCode;
    }
}
