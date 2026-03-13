package com.saraasansor.api.security;

import com.saraasansor.api.model.User;

public class AuthenticatedUserContext {

    public enum AuthScopeType {
        PLATFORM,
        TENANT
    }

    private Long userId;
    private String username;
    private User.Role role;
    private AuthScopeType authScopeType;
    private Long tenantId;
    private String tenantSchema;
    private Long linkedB2bUnitId;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
    }

    public AuthScopeType getAuthScopeType() {
        return authScopeType;
    }

    public void setAuthScopeType(AuthScopeType authScopeType) {
        this.authScopeType = authScopeType;
    }

    public Long getTenantId() {
        return tenantId;
    }

    public void setTenantId(Long tenantId) {
        this.tenantId = tenantId;
    }

    public String getTenantSchema() {
        return tenantSchema;
    }

    public void setTenantSchema(String tenantSchema) {
        this.tenantSchema = tenantSchema;
    }

    public Long getLinkedB2bUnitId() {
        return linkedB2bUnitId;
    }

    public void setLinkedB2bUnitId(Long linkedB2bUnitId) {
        this.linkedB2bUnitId = linkedB2bUnitId;
    }
}
