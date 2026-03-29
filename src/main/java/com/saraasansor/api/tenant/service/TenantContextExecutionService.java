package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.stereotype.Service;

import java.util.function.Supplier;

@Service
public class TenantContextExecutionService {

    private final TenantRepository tenantRepository;
    private final TransactionTemplate tenantReadWriteTemplate;
    private final TransactionTemplate tenantReadOnlyTemplate;

    public TenantContextExecutionService(TenantRepository tenantRepository,
                                         PlatformTransactionManager transactionManager) {
        this.tenantRepository = tenantRepository;

        this.tenantReadWriteTemplate = new TransactionTemplate(transactionManager);
        this.tenantReadWriteTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.tenantReadWriteTemplate.setReadOnly(false);

        this.tenantReadOnlyTemplate = new TransactionTemplate(transactionManager);
        this.tenantReadOnlyTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.tenantReadOnlyTemplate.setReadOnly(true);
    }

    public TenantDescriptor resolveTenantContext(Long tenantId) {
        TenantDescriptor previous = TenantContext.getCurrentTenant();
        TenantContext.clear();
        try {
            return resolveTenantContextInternal(tenantId);
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenant(previous);
            }
        }
    }

    private TenantDescriptor resolveTenantContextInternal(Long tenantId) {
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
        return executeInTenantContextWrite(selectedTenant, callback);
    }

    public <T> T executeInTenantContext(TenantDescriptor selectedTenant, Supplier<T> callback) {
        return executeInTenantContextWrite(selectedTenant, callback);
    }

    public <T> T executeInTenantContextReadOnly(Long tenantId, Supplier<T> callback) {
        TenantDescriptor selectedTenant = resolveTenantContext(tenantId);
        return executeInTenantContextReadOnly(selectedTenant, callback);
    }

    public <T> T executeInTenantContextWrite(Long tenantId, Supplier<T> callback) {
        TenantDescriptor selectedTenant = resolveTenantContext(tenantId);
        return executeInTenantContextWrite(selectedTenant, callback);
    }

    public <T> T executeInTenantContextReadOnly(TenantDescriptor selectedTenant, Supplier<T> callback) {
        return executeInTenantContext(selectedTenant, callback, true);
    }

    public <T> T executeInTenantContextWrite(TenantDescriptor selectedTenant, Supplier<T> callback) {
        return executeInTenantContext(selectedTenant, callback, false);
    }

    private <T> T executeInTenantContext(TenantDescriptor selectedTenant,
                                         Supplier<T> callback,
                                         boolean readOnly) {
        TenantDescriptor previous = TenantContext.getCurrentTenant();
        try {
            TenantContext.setCurrentTenant(selectedTenant);
            TransactionTemplate template = readOnly ? tenantReadOnlyTemplate : tenantReadWriteTemplate;
            return template.execute(status -> callback.get());
        } finally {
            if (previous != null) {
                TenantContext.setCurrentTenant(previous);
            } else {
                TenantContext.clear();
            }
        }
    }
}
