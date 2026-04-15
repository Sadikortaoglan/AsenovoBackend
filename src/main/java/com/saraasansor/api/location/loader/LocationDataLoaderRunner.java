package com.saraasansor.api.location.loader;

import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.repository.TenantRepository;
import com.saraasansor.api.tenant.service.TenantContextExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LocationDataLoaderRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(LocationDataLoaderRunner.class);

    private final LocationDataLoader locationDataLoader;
    private final TenantRepository tenantRepository;
    private final TenantContextExecutionService tenantContextExecutionService;

    public LocationDataLoaderRunner(LocationDataLoader locationDataLoader,
                                    TenantRepository tenantRepository,
                                    TenantContextExecutionService tenantContextExecutionService) {
        this.locationDataLoader = locationDataLoader;
        this.tenantRepository = tenantRepository;
        this.tenantContextExecutionService = tenantContextExecutionService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int failures = 0;

        try {
            log.info("Location data import started for shared/default datasource");
            locationDataLoader.loadIfNeeded();
        } catch (Exception ex) {
            failures++;
            log.error("Location data loading failed. Application will continue without startup location import.", ex);
        }

        List<Tenant> tenants = tenantRepository.findByActiveTrueAndStatusOrderByIdAsc(Tenant.TenantStatus.ACTIVE);
        log.info("Location data tenant import started for {} active tenant(s)", tenants.size());

        for (Tenant tenant : tenants) {
            try {
                TenantDescriptor descriptor = tenantContextExecutionService.resolveTenantContext(tenant.getId());
                tenantContextExecutionService.executeInTenantContextWrite(descriptor, () -> {
                    log.info(
                            "Location data import started for tenant id={} subdomain={} mode={} schema={} db={}",
                            descriptor.getId(),
                            descriptor.getSubdomain(),
                            descriptor.getTenancyMode(),
                            descriptor.getSchemaName(),
                            descriptor.getDbName()
                    );
                    locationDataLoader.loadIfNeeded();
                    return null;
                });
            } catch (Exception ex) {
                failures++;
                log.error(
                        "Location data loading failed for tenant id={} subdomain={} mode={}. Continuing with next tenant.",
                        tenant.getId(),
                        tenant.getSubdomain(),
                        tenant.getTenancyMode(),
                        ex
                );
            }
        }

        log.info("Location data tenant import summary: tenantsChecked={}, failures={}", tenants.size(), failures);
    }
}
