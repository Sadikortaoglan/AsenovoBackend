package com.saraasansor.api.tenant.dto;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class TenantExtendLicenseRequest {

    @NotNull(message = "License end date is required")
    private LocalDate licenseEndDate;

    public LocalDate getLicenseEndDate() {
        return licenseEndDate;
    }

    public void setLicenseEndDate(LocalDate licenseEndDate) {
        this.licenseEndDate = licenseEndDate;
    }
}
