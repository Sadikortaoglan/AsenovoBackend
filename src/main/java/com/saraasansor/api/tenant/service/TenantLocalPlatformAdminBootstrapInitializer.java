package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TenantLocalPlatformAdminBootstrapInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(TenantLocalPlatformAdminBootstrapInitializer.class);

    private final TenantRepository tenantRepository;
    private final TenantSeedService tenantSeedService;

    public TenantLocalPlatformAdminBootstrapInitializer(TenantRepository tenantRepository,
                                                        TenantSeedService tenantSeedService) {
        this.tenantRepository = tenantRepository;
        this.tenantSeedService = tenantSeedService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tenantSeedService.isTenantLocalPlatformAdminBootstrapEnabled()) {
            return;
        }
        if (!tenantSeedService.hasTenantLocalPlatformAdminBootstrapCredentials()) {
            log.warn("Tenant local platform admin repair bootstrap skipped: credentials are not configured");
            return;
        }

        int createdCount = 0;
        for (Tenant tenant : tenantRepository.findAll()) {
            if (tenant.getTenancyMode() != Tenant.TenancyMode.SHARED_SCHEMA) {
                continue;
            }
            if (tenant.getStatus() == Tenant.TenantStatus.DELETED) {
                continue;
            }
            if (tenant.getSchemaName() == null || tenant.getSchemaName().isBlank()) {
                continue;
            }

            try {
                if (tenantSeedService.seedTenantLocalPlatformAdmin(tenant.getSchemaName())) {
                    createdCount++;
                }
            } catch (Exception ex) {
                log.warn(
                        "Tenant local platform admin repair bootstrap failed tenantId={} schema={} reason={}",
                        tenant.getId(),
                        tenant.getSchemaName(),
                        ex.getMessage()
                );
            }
        }

        if (createdCount > 0) {
            log.info("Tenant local platform admin repair bootstrap created {} account(s)", createdCount);
        }
    }
}
