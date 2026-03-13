package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class TenantContextExecutionService {

    private final TenantRepository tenantRepository;

    public TenantContextExecutionService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public TenantDescriptor resolveTenantContext(Long tenantId) {
        Tenant tenant = tenantRepository.findByIdWithPlan(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
        if (tenant.getStatus() == Tenant.TenantStatus.DELETED) {
            throw new RuntimeException("Deleted tenant cannot be selected");
        }

        String planTypeValue = tenant.getPlanType() != null
                ? tenant.getPlanType().name()
                : (tenant.getPlan() != null ? tenant.getPlan().getPlanType().name() : null);

        return new TenantDescriptor(
                tenant.getId(),
                tenant.getCompanyName(),
                tenant.getSubdomain(),
                tenant.getTenancyMode(),
                tenant.getSchemaName(),
                tenant.getDbHost(),
                tenant.getDbName(),
                tenant.getDbUsername(),
                tenant.getDbPassword(),
                tenant.getRedisNamespace(),
                planTypeValue,
                tenant.getStatus(),
                tenant.getLicenseStartDate(),
                tenant.getLicenseEndDate()
        );
    }

    public <T> T executeInTenantContext(Long tenantId, Supplier<T> callback) {
        TenantDescriptor selectedTenant = resolveTenantContext(tenantId);
        return executeInTenantContext(selectedTenant, callback);
    }

    public <T> T executeInTenantContext(TenantDescriptor selectedTenant, Supplier<T> callback) {
        TenantDescriptor previous = TenantContext.getCurrentTenant();
        try {
            TenantContext.setCurrentTenant(selectedTenant);
            return callback.get();
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenant(previous);
            } else {
                TenantContext.clear();
            }
        }
    }
}
