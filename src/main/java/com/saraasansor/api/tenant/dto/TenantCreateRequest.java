package com.saraasansor.api.tenant.dto;

import com.saraasansor.api.tenant.model.Tenant;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.LocalDate;

public class TenantCreateRequest {

    @NotBlank(message = "Company name is required")
    private String companyName;

    @NotBlank(message = "Subdomain is required")
    @Pattern(regexp = "^[a-z0-9-]{2,63}$", message = "Subdomain format is invalid")
    private String subdomain;

    @NotNull(message = "Plan type is required")
    private Tenant.PlanType planType;

    @NotNull(message = "License start date is required")
    private LocalDate licenseStartDate;

    @NotNull(message = "License end date is required")
    private LocalDate licenseEndDate;

    private Integer maxUsers;
    private Integer maxFacilities;
    private Integer maxElevators;
    private String initialAdminUsername;
    private String initialAdminPassword;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
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

    public String getInitialAdminUsername() {
        return initialAdminUsername;
    }

    public void setInitialAdminUsername(String initialAdminUsername) {
        this.initialAdminUsername = initialAdminUsername;
    }

    public String getInitialAdminPassword() {
        return initialAdminPassword;
    }

    public void setInitialAdminPassword(String initialAdminPassword) {
        this.initialAdminPassword = initialAdminPassword;
    }
}
