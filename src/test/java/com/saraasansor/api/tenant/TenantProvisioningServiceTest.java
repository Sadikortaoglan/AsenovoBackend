package com.saraasansor.api.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.tenant.model.Plan;
import com.saraasansor.api.tenant.model.Subscription;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import com.saraasansor.api.tenant.repository.SubscriptionRepository;
import com.saraasansor.api.tenant.repository.TenantRepository;
import com.saraasansor.api.tenant.service.SchemaManagementService;
import com.saraasansor.api.tenant.service.TenantMigrationService;
import com.saraasansor.api.tenant.service.TenantProvisioningJobService;
import com.saraasansor.api.tenant.service.TenantProvisioningService;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import com.saraasansor.api.tenant.service.TenantSeedService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TenantProvisioningServiceTest {

    @Test
    void processCreateTenantJobShouldProvisionSchemaAndActivateTenant() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        TenantProvisioningJobService tenantProvisioningJobService = mock(TenantProvisioningJobService.class);
        SchemaManagementService schemaManagementService = mock(SchemaManagementService.class);
        TenantMigrationService tenantMigrationService = mock(TenantMigrationService.class);
        TenantSeedService tenantSeedService = mock(TenantSeedService.class);
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);

        TenantProvisioningService service = new TenantProvisioningService(
                tenantRepository,
                subscriptionRepository,
                tenantProvisioningJobService,
                schemaManagementService,
                tenantMigrationService,
                tenantSeedService,
                tenantRegistryService,
                new ObjectMapper()
        );

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setPlanType(Plan.PlanType.PRO);

        Tenant tenant = new Tenant();
        tenant.setId(5L);
        tenant.setName("Acme");
        tenant.setCompanyName("Acme");
        tenant.setSubdomain("acme");
        tenant.setPlan(plan);
        tenant.setPlanType(Tenant.PlanType.PROFESSIONAL);
        tenant.setStatus(Tenant.TenantStatus.PENDING);
        tenant.setLicenseStartDate(LocalDate.now().minusDays(1));
        tenant.setLicenseEndDate(LocalDate.now().plusMonths(6));

        TenantProvisioningJob job = new TenantProvisioningJob();
        job.setId(99L);
        job.setTenant(tenant);
        job.setJobType(TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT);
        job.setPayloadJson("{\"initialAdminUsername\":\"admin\",\"initialAdminPassword\":\"password\"}");

        when(tenantRepository.findByIdWithPlan(5L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.existsBySchemaName(anyString())).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findFirstByTenantIdOrderByIdAsc(5L)).thenReturn(Optional.of(new Subscription()));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processJob(job, "worker-1");

        verify(schemaManagementService).createSchemaIfNotExists("tenant_5");
        verify(tenantMigrationService).migrateSchema("tenant_5");
        verify(tenantSeedService).seedTenantLocalPlatformAdmin("tenant_5");
        verify(tenantSeedService).seedInitialAdmin("tenant_5", "admin", "password");
        verify(tenantRegistryService).evictCacheForSubdomain("acme");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository, atLeast(2)).save(tenantCaptor.capture());
        Tenant finalSaved = tenantCaptor.getValue();
        assertThat(finalSaved.getSchemaName()).startsWith("tenant_5");
        assertThat(finalSaved.getStatus()).isEqualTo(Tenant.TenantStatus.ACTIVE);
        assertThat(finalSaved.isActive()).isTrue();
    }

    @Test
    void onJobFailedShouldMarkTenantProvisioningFailedForCreateTenantJobs() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        TenantProvisioningJobService tenantProvisioningJobService = mock(TenantProvisioningJobService.class);
        SchemaManagementService schemaManagementService = mock(SchemaManagementService.class);
        TenantMigrationService tenantMigrationService = mock(TenantMigrationService.class);
        TenantSeedService tenantSeedService = mock(TenantSeedService.class);
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);

        TenantProvisioningService service = new TenantProvisioningService(
                tenantRepository,
                subscriptionRepository,
                tenantProvisioningJobService,
                schemaManagementService,
                tenantMigrationService,
                tenantSeedService,
                tenantRegistryService,
                new ObjectMapper()
        );

        Tenant tenant = new Tenant();
        tenant.setId(6L);
        tenant.setSubdomain("beta");
        tenant.setStatus(Tenant.TenantStatus.PENDING);

        TenantProvisioningJob job = new TenantProvisioningJob();
        job.setId(123L);
        job.setTenant(tenant);
        job.setJobType(TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT);

        when(tenantRepository.findByIdWithPlan(6L)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.onJobFailed(job, new RuntimeException("provisioning failed"), "worker-1");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        Tenant updated = tenantCaptor.getValue();
        assertThat(updated.getStatus()).isEqualTo(Tenant.TenantStatus.PROVISIONING_FAILED);
        assertThat(updated.isActive()).isFalse();
        verify(tenantRegistryService).evictCacheForSubdomain("beta");
        verify(tenantProvisioningJobService).writeAudit(eq(tenant), eq(job), eq("TENANT_PROVISIONING_FAILED"), contains("provisioning failed"), eq("worker-1"));
    }
}
