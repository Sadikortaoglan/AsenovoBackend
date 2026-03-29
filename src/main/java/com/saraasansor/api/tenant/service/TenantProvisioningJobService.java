package com.saraasansor.api.tenant.service;

import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.model.TenantProvisioningAudit;
import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import com.saraasansor.api.tenant.repository.TenantProvisioningAuditRepository;
import com.saraasansor.api.tenant.repository.TenantProvisioningJobRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class TenantProvisioningJobService {

    private final TenantProvisioningJobRepository tenantProvisioningJobRepository;
    private final TenantProvisioningAuditRepository tenantProvisioningAuditRepository;

    public TenantProvisioningJobService(TenantProvisioningJobRepository tenantProvisioningJobRepository,
                                        TenantProvisioningAuditRepository tenantProvisioningAuditRepository) {
        this.tenantProvisioningJobRepository = tenantProvisioningJobRepository;
        this.tenantProvisioningAuditRepository = tenantProvisioningAuditRepository;
    }

    @Transactional
    public TenantProvisioningJob enqueueJob(Tenant tenant,
                                            TenantProvisioningJob.ProvisioningJobType jobType,
                                            String payloadJson,
                                            String createdBy) {
        TenantProvisioningJob job = new TenantProvisioningJob();
        job.setTenant(tenant);
        job.setJobType(jobType);
        job.setStatus(TenantProvisioningJob.ProvisioningJobStatus.PENDING);
        job.setRetryCount(0);
        job.setPayloadJson(payloadJson);
        job.setCreatedBy(createdBy);
        TenantProvisioningJob saved = tenantProvisioningJobRepository.save(job);

        writeAudit(saved.getTenant(), saved, "TENANT_JOB_QUEUED", "Tenant job queued: " + jobType.name(), createdBy);
        return saved;
    }

    @Transactional(readOnly = true)
    public TenantProvisioningJob getJobById(Long id) {
        return tenantProvisioningJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant provisioning job not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<TenantProvisioningJob> getJobs(Long tenantId,
                                               TenantProvisioningJob.ProvisioningJobStatus status,
                                               TenantProvisioningJob.ProvisioningJobType jobType,
                                               Pageable pageable) {
        return tenantProvisioningJobRepository.search(tenantId, status, jobType, pageable);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<TenantProvisioningJob> claimNextPendingJob(String workerNode) {
        Optional<TenantProvisioningJob> jobOpt = tenantProvisioningJobRepository.findNextPendingForUpdate();
        if (jobOpt.isEmpty()) {
            return Optional.empty();
        }

        TenantProvisioningJob job = jobOpt.get();
        job.setStatus(TenantProvisioningJob.ProvisioningJobStatus.IN_PROGRESS);
        job.setStartedAt(LocalDateTime.now());
        job.setFinishedAt(null);
        job.setWorkerNode(workerNode);
        job.setErrorMessage(null);
        TenantProvisioningJob saved = tenantProvisioningJobRepository.save(job);

        writeAudit(saved.getTenant(), saved, "TENANT_JOB_CLAIMED", "Tenant job claimed by worker: " + workerNode, workerNode);
        return Optional.of(saved);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<TenantProvisioningJob> findStaleInProgressJobs(int staleMinutes) {
        if (staleMinutes <= 0) {
            staleMinutes = 30;
        }
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(staleMinutes);
        return tenantProvisioningJobRepository.findStaleInProgressJobs(
                TenantProvisioningJob.ProvisioningJobStatus.IN_PROGRESS,
                cutoff
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(Long jobId, String actor, String message) {
        TenantProvisioningJob job = getJobForUpdate(jobId);
        if (job.getStatus() != TenantProvisioningJob.ProvisioningJobStatus.IN_PROGRESS) {
            return;
        }
        job.setStatus(TenantProvisioningJob.ProvisioningJobStatus.COMPLETED);
        job.setFinishedAt(LocalDateTime.now());
        job.setErrorMessage(null);
        tenantProvisioningJobRepository.save(job);
        writeAudit(job.getTenant(), job, "TENANT_JOB_COMPLETED", message, actor);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long jobId, String actor, String errorMessage) {
        TenantProvisioningJob job = getJobForUpdate(jobId);
        if (job.getStatus() == TenantProvisioningJob.ProvisioningJobStatus.COMPLETED
                || job.getStatus() == TenantProvisioningJob.ProvisioningJobStatus.FAILED
                || job.getStatus() == TenantProvisioningJob.ProvisioningJobStatus.CANCELLED) {
            return;
        }
        job.setStatus(TenantProvisioningJob.ProvisioningJobStatus.FAILED);
        job.setFinishedAt(LocalDateTime.now());
        job.setRetryCount((job.getRetryCount() == null ? 0 : job.getRetryCount()) + 1);
        job.setErrorMessage(errorMessage);
        tenantProvisioningJobRepository.save(job);
        writeAudit(job.getTenant(), job, "TENANT_JOB_FAILED", errorMessage, actor);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeAudit(Tenant tenant,
                           TenantProvisioningJob job,
                           String action,
                           String message,
                           String createdBy) {
        TenantProvisioningAudit audit = new TenantProvisioningAudit();
        audit.setTenant(tenant);
        audit.setJob(job);
        audit.setAction(action);
        audit.setMessage(message);
        audit.setCreatedBy(createdBy);
        tenantProvisioningAuditRepository.save(audit);
    }

    private TenantProvisioningJob getJobForUpdate(Long id) {
        return tenantProvisioningJobRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tenant provisioning job not found: " + id));
    }
}
