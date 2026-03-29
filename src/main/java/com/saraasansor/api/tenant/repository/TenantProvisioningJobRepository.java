package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TenantProvisioningJobRepository extends JpaRepository<TenantProvisioningJob, Long> {

    @Query(value = """
            SELECT * FROM tenant_provisioning_jobs
            WHERE status = 'PENDING'
            ORDER BY requested_at ASC
            LIMIT 1
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    Optional<TenantProvisioningJob> findNextPendingForUpdate();

    @Query("""
            SELECT j FROM TenantProvisioningJob j
            JOIN FETCH j.tenant
            WHERE j.status = :status
              AND j.startedAt IS NOT NULL
              AND j.startedAt < :cutoff
            """)
    List<TenantProvisioningJob> findStaleInProgressJobs(
            @Param("status") TenantProvisioningJob.ProvisioningJobStatus status,
            @Param("cutoff") LocalDateTime cutoff);

    @Query("""
            SELECT j FROM TenantProvisioningJob j
            WHERE (:tenantId IS NULL OR j.tenant.id = :tenantId)
              AND (:status IS NULL OR j.status = :status)
              AND (:jobType IS NULL OR j.jobType = :jobType)
            """)
    Page<TenantProvisioningJob> search(@Param("tenantId") Long tenantId,
                                       @Param("status") TenantProvisioningJob.ProvisioningJobStatus status,
                                       @Param("jobType") TenantProvisioningJob.ProvisioningJobType jobType,
                                       Pageable pageable);
}
