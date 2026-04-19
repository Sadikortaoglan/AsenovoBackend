package com.saraasansor.api.tenant.service;

import com.saraasansor.api.location.loader.LocationDataLoader;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class TenantLocationBootstrapService {

    private static final Logger log = LoggerFactory.getLogger(TenantLocationBootstrapService.class);

    private final TenantContextExecutionService tenantContextExecutionService;
    private final LocationDataLoader locationDataLoader;

    public TenantLocationBootstrapService(TenantContextExecutionService tenantContextExecutionService,
                                          LocationDataLoader locationDataLoader) {
        this.tenantContextExecutionService = tenantContextExecutionService;
        this.locationDataLoader = locationDataLoader;
    }

    public void bootstrapTenantLocationData(Tenant tenant) {
        if (tenant == null) {
            return;
        }
        if (tenant.getTenancyMode() == Tenant.TenancyMode.SHARED_SCHEMA
                && (tenant.getSchemaName() == null || tenant.getSchemaName().isBlank())) {
            log.warn("Tenant location bootstrap skipped for tenantId={} because schema is missing", tenant.getId());
            return;
        }
        if (tenant.getTenancyMode() == Tenant.TenancyMode.DEDICATED_DB
                && (tenant.getDbName() == null || tenant.getDbName().isBlank())) {
            log.warn("Tenant location bootstrap skipped for tenantId={} because dedicated DB config is missing", tenant.getId());
            return;
        }

        TenantDescriptor descriptor = tenantContextExecutionService.resolveTenantContext(tenant.getId());
        tenantContextExecutionService.executeInTenantContextWrite(descriptor, () -> {
            log.info(
                    "Tenant location bootstrap started for tenant id={} subdomain={} mode={} schema={} db={}",
                    descriptor.getId(),
                    descriptor.getSubdomain(),
                    descriptor.getTenancyMode(),
                    descriptor.getSchemaName(),
                    descriptor.getDbName()
            );
            locationDataLoader.loadIfNeeded();
            return null;
        });
    }
}
