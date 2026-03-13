package com.saraasansor.api.tenant.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "tenants")
public class Tenant {

    public enum TenancyMode {
        SHARED_SCHEMA,
        DEDICATED_DB
    }

    public enum TenantStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        EXPIRED,
        DELETED,
        PROVISIONING_FAILED
    }

    public enum PlanType {
        TRIAL,
        BASIC,
        PROFESSIONAL,
        ENTERPRISE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "company_name")
    private String companyName;

    @Column(nullable = false, unique = true)
    private String subdomain;

    @Enumerated(EnumType.STRING)
    @Column(name = "tenancy_mode", nullable = false, length = 32)
    private TenancyMode tenancyMode;

    @Column(name = "schema_name")
    private String schemaName;

    @Column(name = "db_host")
    private String dbHost;

    @Column(name = "db_name")
    private String dbName;

    @Column(name = "db_username")
    private String dbUsername;

    @Column(name = "db_password")
    private String dbPassword;

    @Column(name = "redis_namespace")
    private String redisNamespace;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, length = 32)
    private PlanType planType = PlanType.PROFESSIONAL;

    @Column(name = "license_start_date")
    private LocalDate licenseStartDate;

    @Column(name = "license_end_date")
    private LocalDate licenseEndDate;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_facilities")
    private Integer maxFacilities;

    @Column(name = "max_elevators")
    private Integer maxElevators;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "logo_url", length = 255)
    private String logoUrl;

    @Column(name = "primary_color", length = 7)
    private String primaryColor;

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor;

    @Column(name = "branding_updated_at")
    private LocalDateTime brandingUpdatedAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plan_id", nullable = false)
    private Plan plan;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (isBlank(this.companyName)) {
            this.companyName = this.name;
        }
        if (isBlank(this.name)) {
            this.name = this.companyName;
        }
        if (this.status == TenantStatus.ACTIVE) {
            this.active = true;
        } else if (this.status != null) {
            this.active = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        if (isBlank(this.companyName)) {
            this.companyName = this.name;
        }
        if (isBlank(this.name)) {
            this.name = this.companyName;
        }
        if (this.status == TenantStatus.ACTIVE) {
            this.active = true;
        } else if (this.status != null) {
            this.active = false;
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        if (isBlank(this.companyName)) {
            this.companyName = name;
        }
    }

    public String getCompanyName() {
        return !isBlank(companyName) ? companyName : name;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
        if (isBlank(this.name)) {
            this.name = companyName;
        }
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public TenancyMode getTenancyMode() {
        return tenancyMode;
    }

    public void setTenancyMode(TenancyMode tenancyMode) {
        this.tenancyMode = tenancyMode;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public void setSchemaName(String schemaName) {
        this.schemaName = schemaName;
    }

    public String getDbHost() {
        return dbHost;
    }

    public void setDbHost(String dbHost) {
        this.dbHost = dbHost;
    }

    public String getDbName() {
        return dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    public String getRedisNamespace() {
        return redisNamespace;
    }

    public void setRedisNamespace(String redisNamespace) {
        this.redisNamespace = redisNamespace;
    }

    public TenantStatus getStatus() {
        return status;
    }

    public void setStatus(TenantStatus status) {
        this.status = status;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public LocalDate getLicenseStartDate() {
        return licenseStartDate;
    }

    public void setLicenseStartDate(LocalDate licenseStartDate) {
        this.licenseStartDate = licenseStartDate;
    }

    public LocalDate getLicenseEndDate() {
        return licenseEndDate;
    }

    public void setLicenseEndDate(LocalDate licenseEndDate) {
        this.licenseEndDate = licenseEndDate;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxFacilities() {
        return maxFacilities;
    }

    public void setMaxFacilities(Integer maxFacilities) {
        this.maxFacilities = maxFacilities;
    }

    public Integer getMaxElevators() {
        return maxElevators;
    }

    public void setMaxElevators(Integer maxElevators) {
        this.maxElevators = maxElevators;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public LocalDateTime getBrandingUpdatedAt() {
        return brandingUpdatedAt;
    }

    public void setBrandingUpdatedAt(LocalDateTime brandingUpdatedAt) {
        this.brandingUpdatedAt = brandingUpdatedAt;
    }

    public Plan getPlan() {
        return plan;
    }

    public void setPlan(Plan plan) {
        this.plan = plan;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
