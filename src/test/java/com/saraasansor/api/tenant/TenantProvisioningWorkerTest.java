package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import com.saraasansor.api.tenant.service.TenantProvisioningJobService;
import com.saraasansor.api.tenant.service.TenantProvisioningService;
import com.saraasansor.api.tenant.service.TenantProvisioningWorker;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

class TenantProvisioningWorkerTest {

    @Test
    void workerShouldMarkJobFailedWhenProvisioningThrows() {
        TenantProvisioningJobService jobService = mock(TenantProvisioningJobService.class);
        TenantProvisioningService provisioningService = mock(TenantProvisioningService.class);

        TenantProvisioningWorker worker = new TenantProvisioningWorker(jobService, provisioningService);
        ReflectionTestUtils.setField(worker, "workerEnabled", true);
        ReflectionTestUtils.setField(worker, "maxJobsPerCycle", 1);
        ReflectionTestUtils.setField(worker, "staleInProgressMinutes", 30);

        Tenant tenant = new Tenant();
        tenant.setId(1L);
        tenant.setSubdomain("acme");

        TenantProvisioningJob job = new TenantProvisioningJob();
        job.setId(11L);
        job.setTenant(tenant);
        job.setJobType(TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT);

        when(jobService.claimNextPendingJob(anyString())).thenReturn(Optional.of(job));
        when(jobService.findStaleInProgressJobs(anyInt())).thenReturn(List.of());
        doThrow(new RuntimeException("boom")).when(provisioningService).processJob(eq(job), anyString());

        worker.processPendingJobs();

        verify(provisioningService, times(1)).onJobFailed(eq(job), any(RuntimeException.class), anyString());
        verify(jobService, times(1)).markFailed(eq(11L), anyString(), eq("boom"));
    }
}
