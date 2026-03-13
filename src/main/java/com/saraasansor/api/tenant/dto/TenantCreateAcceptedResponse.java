package com.saraasansor.api.tenant.dto;

public class TenantCreateAcceptedResponse {

    private TenantResponse tenant;
    private TenantProvisioningJobResponse job;

    public TenantResponse getTenant() {
        return tenant;
    }

    public void setTenant(TenantResponse tenant) {
        this.tenant = tenant;
    }

    public TenantProvisioningJobResponse getJob() {
        return job;
    }

    public void setJob(TenantProvisioningJobResponse job) {
        this.job = job;
    }
}
