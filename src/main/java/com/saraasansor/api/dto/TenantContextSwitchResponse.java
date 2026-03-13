package com.saraasansor.api.dto;

import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;

import java.time.LocalDate;

public class TenantContextSwitchResponse {

    private Long tenantId;
    private String subdomain;
    private String schemaName;
    private Tenant.TenantStatus status;
    private LocalDate licenseStartDate;
    private LocalDate licenseEndDate;

    public static TenantContextSwitchResponse fromDescriptor(TenantDescriptor descriptor) {
        TenantContextSwitchResponse response = new TenantContextSwitchResponse();
        response.setTenantId(descriptor.getId());
        response.setSubdomain(descriptor.getSubdomain());
        response.setSchemaName(descriptor.getSchemaName());
        response.setStatus(descriptor.getStatus());
        response.setLicenseStartDate(descriptor.getLicenseStartDate());
        response.setLicenseEndDate(descriptor.getLicenseEndDate());
        return response;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
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
}
