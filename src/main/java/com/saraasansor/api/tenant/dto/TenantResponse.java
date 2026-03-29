package com.saraasansor.api.tenant.dto;

import com.saraasansor.api.tenant.model.Tenant;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TenantResponse {

    private Long id;
    private String companyName;
    private String subdomain;
    private String schemaName;
    private Tenant.TenantStatus status;
    private Tenant.PlanType planType;
    private LocalDate licenseStartDate;
    private LocalDate licenseEndDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;

    public static TenantResponse fromEntity(Tenant tenant) {
        TenantResponse response = new TenantResponse();
        response.setId(tenant.getId());
        response.setCompanyName(tenant.getCompanyName());
        response.setSubdomain(tenant.getSubdomain());
        response.setSchemaName(tenant.getSchemaName());
        response.setStatus(tenant.getStatus());
        response.setPlanType(tenant.getPlanType());
        response.setLicenseStartDate(tenant.getLicenseStartDate());
        response.setLicenseEndDate(tenant.getLicenseEndDate());
        response.setCreatedAt(tenant.getCreatedAt());
        response.setUpdatedAt(tenant.getUpdatedAt());
        response.setCreatedBy(tenant.getCreatedBy());
        response.setUpdatedBy(tenant.getUpdatedBy());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public Tenant.TenantStatus getStatus() {
        return status;
    }

    public void setStatus(Tenant.TenantStatus status) {
        this.status = status;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
