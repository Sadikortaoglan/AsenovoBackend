package com.saraasansor.api.tenant.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.exception.NotFoundException;
import com.saraasansor.api.exception.ValidationException;
import com.saraasansor.api.tenant.model.Subscription;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.model.TenantProvisioningJob;
import com.saraasansor.api.tenant.repository.SubscriptionRepository;
import com.saraasansor.api.tenant.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Service
public class TenantProvisioningService {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TenantProvisioningJobService tenantProvisioningJobService;
    private final SchemaManagementService schemaManagementService;
    private final TenantMigrationService tenantMigrationService;
    private final TenantSeedService tenantSeedService;
    private final TenantRegistryService tenantRegistryService;
    private final ObjectMapper objectMapper;

    public TenantProvisioningService(TenantRepository tenantRepository,
                                     SubscriptionRepository subscriptionRepository,
                                     TenantProvisioningJobService tenantProvisioningJobService,
                                     SchemaManagementService schemaManagementService,
                                     TenantMigrationService tenantMigrationService,
                                     TenantSeedService tenantSeedService,
                                     TenantRegistryService tenantRegistryService,
                                     ObjectMapper objectMapper) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantProvisioningJobService = tenantProvisioningJobService;
        this.schemaManagementService = schemaManagementService;
        this.tenantMigrationService = tenantMigrationService;
        this.tenantSeedService = tenantSeedService;
        this.tenantRegistryService = tenantRegistryService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void processJob(TenantProvisioningJob job, String actor) {
        switch (job.getJobType()) {
            case CREATE_TENANT -> processCreateTenant(job, actor);
            case SUSPEND_TENANT -> processSuspendTenant(job, actor);
            case ACTIVATE_TENANT -> processActivateTenant(job, actor);
            case EXTEND_LICENSE -> processExtendLicense(job, actor);
            case DELETE_TENANT -> processDeleteTenant(job, actor);
            case REBUILD_SCHEMA -> processRebuildSchema(job, actor);
            default -> throw new ValidationException("Unsupported provisioning job type: " + job.getJobType());
        }
    }

    @Transactional
    public void onJobFailed(TenantProvisioningJob job, Exception exception, String actor) {
        if (job.getJobType() == TenantProvisioningJob.ProvisioningJobType.CREATE_TENANT) {
            Tenant tenant = findTenant(job.getTenant().getId());
            tenant.setStatus(Tenant.TenantStatus.PROVISIONING_FAILED);
            tenant.setActive(false);
            tenant.setUpdatedBy(actor);
            tenant.setUpdatedAt(LocalDateTime.now());
            tenantRepository.save(tenant);
            tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
        }

        tenantProvisioningJobService.writeAudit(
                job.getTenant(),
                job,
                "TENANT_PROVISIONING_FAILED",
                exception.getMessage(),
                actor
        );
    }

    private void processCreateTenant(TenantProvisioningJob job, String actor) {
        Tenant tenant = findTenant(job.getTenant().getId());
        Map<String, Object> payload = parsePayload(job.getPayloadJson());

        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_PROVISIONING_STARTED", "Provisioning started", actor);

        String schemaName = generateSchemaName(tenant.getId());
        schemaName = ensureUniqueSchemaName(tenant, schemaName);
        tenant.setSchemaName(schemaName);
        tenant.setUpdatedBy(actor);
        tenantRepository.save(tenant);

        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_SCHEMA_ASSIGNED", "Schema assigned: " + schemaName, actor);

        schemaManagementService.createSchemaIfNotExists(schemaName);
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_SCHEMA_CREATED", "Schema created: " + schemaName, actor);

        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_MIGRATION_STARTED", "Tenant migration started", actor);
        tenantMigrationService.migrateSchema(schemaName);
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_MIGRATION_COMPLETED", "Tenant migration completed", actor);

        String initialAdminUsername = asString(payload.get("initialAdminUsername"));
        String initialAdminPassword = asString(payload.get("initialAdminPassword"));
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_SEED_STARTED", "Tenant seed started", actor);
        tenantSeedService.seedInitialAdmin(schemaName, initialAdminUsername, initialAdminPassword);
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_SEED_COMPLETED", "Tenant seed completed", actor);

        if (tenant.getLicenseEndDate() != null && LocalDate.now().isAfter(tenant.getLicenseEndDate())) {
            tenant.setStatus(Tenant.TenantStatus.EXPIRED);
            tenant.setActive(false);
        } else {
            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
            tenant.setActive(true);
        }
        tenant.setUpdatedBy(actor);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);

        synchronizeSubscription(tenant);
        tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_ACTIVATION_COMPLETED", "Tenant activation completed", actor);
    }

    private void processSuspendTenant(TenantProvisioningJob job, String actor) {
        Tenant tenant = findTenant(job.getTenant().getId());
        tenant.setStatus(Tenant.TenantStatus.SUSPENDED);
        tenant.setActive(false);
        tenant.setUpdatedBy(actor);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        synchronizeSubscription(tenant);
        tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_SUSPENDED", "Tenant suspended", actor);
    }

    private void processActivateTenant(TenantProvisioningJob job, String actor) {
        Tenant tenant = findTenant(job.getTenant().getId());
        if (tenant.getStatus() == Tenant.TenantStatus.DELETED) {
            throw new ValidationException("Deleted tenant cannot be activated");
        }
        if (tenant.getLicenseEndDate() != null && LocalDate.now().isAfter(tenant.getLicenseEndDate())) {
            tenant.setStatus(Tenant.TenantStatus.EXPIRED);
            tenant.setActive(false);
            tenantRepository.save(tenant);
            throw new ValidationException("Tenant license is expired. Extend license before activation");
        }
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        tenant.setActive(true);
        tenant.setUpdatedBy(actor);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        synchronizeSubscription(tenant);
        tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_ACTIVATED", "Tenant activated", actor);
    }

    private void processExtendLicense(TenantProvisioningJob job, String actor) {
        Tenant tenant = findTenant(job.getTenant().getId());
        Map<String, Object> payload = parsePayload(job.getPayloadJson());
        LocalDate newLicenseEndDate = parseLocalDate(payload.get("licenseEndDate"));
        if (newLicenseEndDate == null) {
            throw new ValidationException("licenseEndDate is required");
        }
        if (tenant.getLicenseStartDate() != null && newLicenseEndDate.isBefore(tenant.getLicenseStartDate())) {
            throw new ValidationException("licenseEndDate cannot be before licenseStartDate");
        }

        tenant.setLicenseEndDate(newLicenseEndDate);
        if (tenant.getStatus() == Tenant.TenantStatus.EXPIRED
                && (tenant.getLicenseEndDate() == null || !LocalDate.now().isAfter(tenant.getLicenseEndDate()))) {
            tenant.setStatus(Tenant.TenantStatus.ACTIVE);
            tenant.setActive(true);
        }
        tenant.setUpdatedBy(actor);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        synchronizeSubscription(tenant);
        tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_LICENSE_EXTENDED",
                "Tenant license extended until " + newLicenseEndDate, actor);
    }

    private void processDeleteTenant(TenantProvisioningJob job, String actor) {
        Tenant tenant = findTenant(job.getTenant().getId());
        tenant.setStatus(Tenant.TenantStatus.DELETED);
        tenant.setActive(false);
        tenant.setUpdatedBy(actor);
        tenant.setUpdatedAt(LocalDateTime.now());
        tenantRepository.save(tenant);
        synchronizeSubscription(tenant);
        tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_SOFT_DELETED", "Tenant soft deleted", actor);
    }

    private void processRebuildSchema(TenantProvisioningJob job, String actor) {
        Tenant tenant = findTenant(job.getTenant().getId());
        String schemaName = tenant.getSchemaName();
        if (schemaName == null || schemaName.isBlank()) {
            throw new ValidationException("Tenant schema is missing");
        }

        schemaManagementService.createSchemaIfNotExists(schemaName);
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_REBUILD_SCHEMA_STARTED", "Schema rebuild started", actor);
        tenantMigrationService.migrateSchema(schemaName);
        tenantProvisioningJobService.writeAudit(tenant, job, "TENANT_REBUILD_SCHEMA_COMPLETED", "Schema rebuild completed", actor);
    }

    private void synchronizeSubscription(Tenant tenant) {
        Subscription subscription = subscriptionRepository.findFirstByTenantIdOrderByIdAsc(tenant.getId())
                .orElseGet(Subscription::new);
        subscription.setTenant(tenant);
        subscription.setPlan(tenant.getPlan());
        subscription.setStartsAt(tenant.getLicenseStartDate());
        subscription.setEndsAt(tenant.getLicenseEndDate());
        subscription.setAutoRenew(false);
        subscription.setActive(tenant.getStatus() == Tenant.TenantStatus.ACTIVE);
        subscriptionRepository.save(subscription);
    }

    private Tenant findTenant(Long tenantId) {
        return tenantRepository.findByIdWithPlan(tenantId)
                .orElseThrow(() -> new NotFoundException("Tenant not found: " + tenantId));
    }

    private String generateSchemaName(Long tenantId) {
        return "tenant_" + tenantId;
    }

    private String ensureUniqueSchemaName(Tenant tenant, String baseName) {
        String candidate = baseName;
        int sequence = 1;
        while (tenantRepository.existsBySchemaName(candidate)
                && (tenant.getSchemaName() == null || !candidate.equals(tenant.getSchemaName()))) {
            candidate = baseName + "_" + sequence;
            sequence++;
        }
        return candidate;
    }

    private Map<String, Object> parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(payloadJson, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new ValidationException("Tenant job payload cannot be parsed");
        }
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private LocalDate parseLocalDate(Object value) {
        if (value == null) {
            return null;
        }
        String raw = String.valueOf(value).trim();
        if (raw.isEmpty()) {
            return null;
        }
        return LocalDate.parse(raw);
    }
}
