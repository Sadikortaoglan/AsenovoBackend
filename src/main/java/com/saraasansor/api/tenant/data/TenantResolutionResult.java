package com.saraasansor.api.tenant.data;

public class TenantResolutionResult {

    public enum ResolutionStatus {
        RESOLVED,
        TENANT_NOT_FOUND,
        SUSPENDED,
        EXPIRED,
        PENDING,
        PROVISIONING_FAILED,
        DELETED,
        INACTIVE
    }

    private final ResolutionStatus status;
    private final TenantDescriptor tenantDescriptor;
    private final String message;

    private TenantResolutionResult(ResolutionStatus status, TenantDescriptor tenantDescriptor, String message) {
        this.status = status;
        this.tenantDescriptor = tenantDescriptor;
        this.message = message;
    }

    public static TenantResolutionResult resolved(TenantDescriptor descriptor) {
        return new TenantResolutionResult(ResolutionStatus.RESOLVED, descriptor, null);
    }

    public static TenantResolutionResult blocked(ResolutionStatus status, String message) {
        return new TenantResolutionResult(status, null, message);
    }

    public ResolutionStatus getStatus() {
        return status;
    }

    public TenantDescriptor getTenantDescriptor() {
        return tenantDescriptor;
    }

    public String getMessage() {
        return message;
    }
}
