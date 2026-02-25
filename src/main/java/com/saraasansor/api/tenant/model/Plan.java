package com.saraasansor.api.tenant.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

@Entity
@Table(name = "plans")
public class Plan {

    public enum PlanType {
        PRO,
        ENTERPRISE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", nullable = false, columnDefinition = "plan_type")
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private PlanType planType = PlanType.PRO;

    @Column(name = "max_users")
    private Integer maxUsers;

    @Column(name = "max_assets")
    private Integer maxAssets;

    @Column(name = "api_rate_limit_per_minute")
    private Integer apiRateLimitPerMinute;

    @Column(name = "max_storage_mb")
    private Integer maxStorageMb;

    @Column(name = "priority_support")
    private boolean prioritySupport;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public PlanType getPlanType() {
        return planType;
    }

    public void setPlanType(PlanType planType) {
        this.planType = planType;
    }

    public Integer getApiRateLimitPerMinute() {
        return apiRateLimitPerMinute;
    }

    public void setApiRateLimitPerMinute(Integer apiRateLimitPerMinute) {
        this.apiRateLimitPerMinute = apiRateLimitPerMinute;
    }

    public Integer getMaxUsers() {
        return maxUsers;
    }

    public void setMaxUsers(Integer maxUsers) {
        this.maxUsers = maxUsers;
    }

    public Integer getMaxAssets() {
        return maxAssets;
    }

    public void setMaxAssets(Integer maxAssets) {
        this.maxAssets = maxAssets;
    }

    public Integer getMaxStorageMb() {
        return maxStorageMb;
    }

    public void setMaxStorageMb(Integer maxStorageMb) {
        this.maxStorageMb = maxStorageMb;
    }

    public boolean isPrioritySupport() {
        return prioritySupport;
    }

    public void setPrioritySupport(boolean prioritySupport) {
        this.prioritySupport = prioritySupport;
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
}

