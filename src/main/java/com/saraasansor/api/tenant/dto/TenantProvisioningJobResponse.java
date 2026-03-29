package com.saraasansor.api.tenant.dto;

import com.saraasansor.api.tenant.model.TenantProvisioningJob;

import java.time.LocalDateTime;

public class TenantProvisioningJobResponse {

    private Long id;
    private Long tenantId;
    private TenantProvisioningJob.ProvisioningJobType jobType;
    private TenantProvisioningJob.ProvisioningJobStatus status;
    private LocalDateTime requestedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private Integer retryCount;
    private String errorMessage;

    public static TenantProvisioningJobResponse fromEntity(TenantProvisioningJob entity) {
        TenantProvisioningJobResponse response = new TenantProvisioningJobResponse();
        response.setId(entity.getId());
        response.setTenantId(entity.getTenant() != null ? entity.getTenant().getId() : null);
        response.setJobType(entity.getJobType());
        response.setStatus(entity.getStatus());
        response.setRequestedAt(entity.getRequestedAt());
        response.setStartedAt(entity.getStartedAt());
        response.setFinishedAt(entity.getFinishedAt());
        response.setRetryCount(entity.getRetryCount());
        response.setErrorMessage(entity.getErrorMessage());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public TenantProvisioningJob.ProvisioningJobType getJobType() {
        return jobType;
    }

    public void setJobType(TenantProvisioningJob.ProvisioningJobType jobType) {
        this.jobType = jobType;
    }

    public TenantProvisioningJob.ProvisioningJobStatus getStatus() {
        return status;
    }

    public void setStatus(TenantProvisioningJob.ProvisioningJobStatus status) {
        this.status = status;
    }

    public LocalDateTime getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(LocalDateTime requestedAt) {
        this.requestedAt = requestedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(LocalDateTime finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
