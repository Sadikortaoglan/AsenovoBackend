package com.saraasansor.api.tenant.service;

import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.util.Optional;
import java.util.UUID;

@Component
public class TenantProvisioningWorker {

    private static final Logger log = LoggerFactory.getLogger(TenantProvisioningWorker.class);

    private final TenantProvisioningJobService tenantProvisioningJobService;
    private final TenantProvisioningService tenantProvisioningService;
    private final String workerNodeId;

    @Value("${app.tenancy.provisioning.worker.enabled:true}")
    private boolean workerEnabled;

    @Value("${app.tenancy.provisioning.worker.max-jobs-per-cycle:5}")
    private int maxJobsPerCycle;

    @Value("${app.tenancy.provisioning.worker.stale-in-progress-minutes:30}")
    private int staleInProgressMinutes;

    public TenantProvisioningWorker(TenantProvisioningJobService tenantProvisioningJobService,
                                    TenantProvisioningService tenantProvisioningService) {
        this.tenantProvisioningJobService = tenantProvisioningJobService;
        this.tenantProvisioningService = tenantProvisioningService;
        this.workerNodeId = resolveWorkerNodeId();
    }

    @Scheduled(fixedDelayString = "${app.tenancy.provisioning.worker.delay-ms:5000}")
    public void processPendingJobs() {
        if (!workerEnabled) {
            return;
        }

        for (int i = 0; i < maxJobsPerCycle; i++) {
            Optional<TenantProvisioningJob> claimed = tenantProvisioningJobService.claimNextPendingJob(workerNodeId);
            if (claimed.isEmpty()) {
                return;
            }

            TenantProvisioningJob job = claimed.get();
            try {
                tenantProvisioningService.processJob(job, workerNodeId);
                tenantProvisioningJobService.markCompleted(job.getId(), workerNodeId, "Provisioning job completed");
            } catch (Throwable ex) {
                log.error("Tenant provisioning job {} failed: {}", job.getId(), ex.getMessage(), ex);
                Exception wrapped = ex instanceof Exception ? (Exception) ex : new RuntimeException(ex.getMessage(), ex);
                try {
                    tenantProvisioningService.onJobFailed(job, wrapped, workerNodeId);
                } catch (Exception failureHandlerException) {
                    log.error("Tenant provisioning failure handler crashed for job {}: {}",
                            job.getId(), failureHandlerException.getMessage(), failureHandlerException);
                }
                try {
                    String errorMessage = wrapped.getMessage() != null ? wrapped.getMessage() : "Tenant provisioning failed";
                    tenantProvisioningJobService.markFailed(job.getId(), workerNodeId, errorMessage);
                } catch (Exception markFailedException) {
                    log.error("Failed to mark tenant provisioning job {} as FAILED: {}",
                            job.getId(), markFailedException.getMessage(), markFailedException);
                }
            }
        }
    }

    @Scheduled(fixedDelayString = "${app.tenancy.provisioning.worker.watchdog-delay-ms:60000}")
    public void recoverStaleInProgressJobs() {
        if (!workerEnabled) {
            return;
        }

        try {
            for (TenantProvisioningJob staleJob : tenantProvisioningJobService.findStaleInProgressJobs(staleInProgressMinutes)) {
                String timeoutMessage = "Job timeout watchdog triggered after " + staleInProgressMinutes + " minute(s)";
                log.warn("Tenant provisioning job {} timed out. Marking as FAILED.", staleJob.getId());
                RuntimeException timeoutException = new RuntimeException(timeoutMessage);
                tenantProvisioningService.onJobFailed(staleJob, timeoutException, workerNodeId);
                tenantProvisioningJobService.markFailed(staleJob.getId(), workerNodeId, timeoutMessage);
            }
        } catch (Exception ex) {
            log.error("Failed to process stale IN_PROGRESS tenant jobs: {}", ex.getMessage(), ex);
        }
    }

    private String resolveWorkerNodeId() {
        try {
            String host = InetAddress.getLocalHost().getHostName();
            return host + "-" + UUID.randomUUID();
        } catch (Exception ignored) {
            return "worker-" + UUID.randomUUID();
        }
    }
}
