package com.saraasansor.api.tenant;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.tenant.dto.TenantCreateAcceptedResponse;
import com.saraasansor.api.tenant.dto.TenantCreateRequest;
import com.saraasansor.api.tenant.model.Plan;
import com.saraasansor.api.tenant.model.Subscription;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import com.saraasansor.api.tenant.repository.PlanRepository;
import com.saraasansor.api.tenant.repository.SubscriptionRepository;
import com.saraasansor.api.tenant.repository.TenantRepository;
import com.saraasansor.api.tenant.service.TenantManagementService;
import com.saraasansor.api.tenant.service.TenantProvisioningJobService;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TenantManagementServiceTest {

    @Test
    void createTenantShouldCreatePendingTenantAndQueueProvisioningJob() {
        TenantRepository tenantRepository = mock(TenantRepository.class);
        PlanRepository planRepository = mock(PlanRepository.class);
        SubscriptionRepository subscriptionRepository = mock(SubscriptionRepository.class);
        TenantProvisioningJobService tenantProvisioningJobService = mock(TenantProvisioningJobService.class);
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);

        TenantManagementService service = new TenantManagementService(
                tenantRepository,
                planRepository,
                subscriptionRepository,
                tenantProvisioningJobService,
                tenantRegistryService,
                new ObjectMapper()
        );

        Plan plan = new Plan();
        plan.setId(1L);
        plan.setPlanType(Plan.PlanType.PRO);
        plan.setCode("PRO-DEFAULT");

        when(planRepository.findFirstByPlanTypeAndActiveTrueOrderByIdAsc(Plan.PlanType.PRO))
                .thenReturn(Optional.of(plan));
        when(tenantRepository.existsBySubdomain("acme")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> {
            Tenant tenant = invocation.getArgument(0);
            tenant.setId(100L);
            return tenant;
        });
        when(subscriptionRepository.findFirstByTenantIdOrderByIdAsc(100L)).thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(tenantProvisioningJobService.enqueueJob(any(Tenant.class), eq(TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT), any(), any()))
                .thenAnswer(invocation -> {
                    TenantProvisioningJob job = new TenantProvisioningJob();
                    job.setId(9L);
                    job.setTenant(invocation.getArgument(0));
                    job.setJobType(TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT);
                    job.setStatus(TenantProvisioningJob.ProvisioningJobStatus.PENDING);
                    return job;
                });

        TenantCreateRequest request = new TenantCreateRequest();
        request.setCompanyName("Acme Inc.");
        request.setSubdomain("acme");
        request.setPlanType(Tenant.PlanType.BASIC);
        request.setLicenseStartDate(LocalDate.now());
        request.setLicenseEndDate(LocalDate.now().plusMonths(6));
        request.setInitialAdminUsername("admin_acme");
        request.setInitialAdminPassword("password123");

        TenantCreateAcceptedResponse response = service.createTenant(request);

        assertThat(response).isNotNull();
        assertThat(response.getTenant()).isNotNull();
        assertThat(response.getTenant().getId()).isEqualTo(100L);
        assertThat(response.getTenant().getStatus()).isEqualTo(Tenant.TenantStatus.PENDING);
        assertThat(response.getTenant().getSubdomain()).isEqualTo("acme");
        assertThat(response.getJob()).isNotNull();
        assertThat(response.getJob().getJobType()).isEqualTo(TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT);
        assertThat(response.getJob().getStatus()).isEqualTo(TenantProvisioningJob.ProvisioningJobStatus.PENDING);

        verify(tenantProvisioningJobService, times(1))
                .enqueueJob(any(Tenant.class), eq(TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT), any(), any());
    }
}
