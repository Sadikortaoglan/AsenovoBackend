package com.saraasansor.api.dto.auth;

public class LoginResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private Long userId;
    private String username;
    private String role;
    private String userType;
    private Long b2bUnitId;
    private String authScopeType;
    private Long tenantId;
    private String tenantSchema;

    public LoginResponse() {
    }

    public LoginResponse(String accessToken, String refreshToken, String tokenType, Long userId, String username, String role, String userType, Long b2bUnitId, String authScopeType, Long tenantId, String tenantSchema) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = tokenType;
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.userType = userType;
        this.b2bUnitId = b2bUnitId;
        this.authScopeType = authScopeType;
        this.tenantId = tenantId;
        this.tenantSchema = tenantSchema;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Long getB2bUnitId() {
        return b2bUnitId;
    }

    public void setB2bUnitId(Long b2bUnitId) {
        this.b2bUnitId = b2bUnitId;
    }

    public String getAuthScopeType() {
        return authScopeType;
    }

    public void setAuthScopeType(String authScopeType) {
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
}
