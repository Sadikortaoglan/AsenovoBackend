package com.saraasansor.api.tenant.data;

public class PlanSnapShot {

    private Long id;
    private String code;
    private String planType;
    private Integer maxUsers;
    private Integer maxAssets;
    private Integer apiRateLimitPerMinute;
    private Integer maxStorageMb;
    private boolean prioritySupport;
    private boolean active = true;

    public PlanSnapShot(Long id, String code, String planType, Integer maxUsers, Integer maxAssets, Integer apiRateLimitPerMinute, Integer maxStorageMb, boolean prioritySupport, boolean active) {
        this.id = id;
        this.code = code;
        this.planType = planType;
        this.maxUsers = maxUsers;
        this.maxAssets = maxAssets;
        this.apiRateLimitPerMinute = apiRateLimitPerMinute;
        this.maxStorageMb = maxStorageMb;
        this.prioritySupport = prioritySupport;
        this.active = active;
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

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
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

    public Integer getApiRateLimitPerMinute() {
        return apiRateLimitPerMinute;
    }

    public void setApiRateLimitPerMinute(Integer apiRateLimitPerMinute) {
        this.apiRateLimitPerMinute = apiRateLimitPerMinute;
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
}
