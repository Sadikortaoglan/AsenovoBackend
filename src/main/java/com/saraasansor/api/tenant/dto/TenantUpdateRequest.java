package com.saraasansor.api.tenant.dto;

import com.saraasansor.api.tenant.model.Tenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class TenantUpdateRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotNull(message = "Plan type is required")
    private Tenant.PlanType planType;

    @NotNull(message = "License start date is required")
    private LocalDate licenseStartDate;

    @NotNull(message = "License end date is required")
    private LocalDate licenseEndDate;

    private Tenant.TenantStatus status;
    private Integer maxUsers;
    private Integer maxFacilities;
    private Integer maxElevators;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public Tenant.PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(Tenant.PlanType planType) {
        this.planType = planType;
    }

    public LocalDate getLicenseStartDate() {
        return licenseStartDate;
    }

    public void setLicenseStartDate(LocalDate licenseStartDate) {
        this.licenseStartDate = licenseStartDate;
    }

    public LocalDate getLicenseEndDate() {
        return licenseEndDate;
    }

    public void setLicenseEndDate(LocalDate licenseEndDate) {
        this.licenseEndDate = licenseEndDate;
    }

    public Tenant.TenantStatus getStatus() {
        return status;
    }

    public void setStatus(Tenant.TenantStatus status) {
        this.status = status;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxFacilities() {
        return maxFacilities;
    }

    public void setMaxFacilities(Integer maxFacilities) {
        this.maxFacilities = maxFacilities;
    }

    public Integer getMaxElevators() {
        return maxElevators;
    }

    public void setMaxElevators(Integer maxElevators) {
        this.maxElevators = maxElevators;
    }
}
