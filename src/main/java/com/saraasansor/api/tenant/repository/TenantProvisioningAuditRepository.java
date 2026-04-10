package com.saraasansor.api.tenant.repository;

import com.saraasansor.api.tenant.model.TenantProvisioningAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantProvisioningAuditRepository extends JpaRepository<TenantProvisioningAudit, Long> {
}
