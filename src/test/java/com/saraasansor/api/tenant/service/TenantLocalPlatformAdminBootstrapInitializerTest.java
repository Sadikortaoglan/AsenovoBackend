package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TenantLocalPlatformAdminBootstrapInitializerTest {

    @Test
    void shouldBootstrapOnlySharedSchemaNonDeletedTenants() throws Exception {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        TenantSeedService tenantSeedService = mock(TenantSeedService.class);
        TenantLocalPlatformAdminBootstrapInitializer initializer =
                new TenantLocalPlatformAdminBootstrapInitializer(tenantRepository, tenantSeedService);

        Tenant sharedActive = tenant(1L, Tenant.TenancyMode.SHARED_SCHEMA, Tenant.TenantStatus.ACTIVE, "tenant_1");
        Tenant sharedDeleted = tenant(2L, Tenant.TenancyMode.SHARED_SCHEMA, Tenant.TenantStatus.DELETED, "tenant_2");
        Tenant dedicated = tenant(3L, Tenant.TenancyMode.DEDICATED_DB, Tenant.TenantStatus.ACTIVE, "public");
        Tenant noSchema = tenant(4L, Tenant.TenancyMode.SHARED_SCHEMA, Tenant.TenantStatus.ACTIVE, "");
        when(tenantRepository.findAll()).thenReturn(List.of(sharedActive, sharedDeleted, dedicated, noSchema));
        when(tenantSeedService.isTenantLocalPlatformAdminBootstrapEnabled()).thenReturn(true);
        when(tenantSeedService.hasTenantLocalPlatformAdminBootstrapCredentials()).thenReturn(true);

        initializer.run(null);

        verify(tenantSeedService).seedTenantLocalPlatformAdmin("tenant_1");
        verify(tenantSeedService, never()).seedTenantLocalPlatformAdmin("tenant_2");
        verify(tenantSeedService, never()).seedTenantLocalPlatformAdmin("public");
    }

    private Tenant tenant(Long id,
                          Tenant.TenancyMode mode,
                          Tenant.TenantStatus status,
                          String schemaName) {
        Tenant tenant = new Tenant();
        tenant.setId(id);
        tenant.setSubdomain("tenant" + id);
        tenant.setTenancyMode(mode);
        tenant.setStatus(status);
        tenant.setSchemaName(schemaName);
        return tenant;
    }
}
