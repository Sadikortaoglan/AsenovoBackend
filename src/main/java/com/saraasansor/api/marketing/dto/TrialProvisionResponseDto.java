package com.saraasansor.api.marketing.dto;

import java.time.OffsetDateTime;

public class TrialProvisionResponseDto {
    private String requestToken;
    private String tenantSlug;
    private String tenantDatabase;
    private String loginUrl;
    private OffsetDateTime expiresAt;
    private String status;
    private String username;
    private String temporaryPassword;
    private String provisioningError;
    private Boolean existingDemo;
    private Boolean accessEmailSent;
    private String emailError;
    private Boolean showTemporaryPassword;

    public String getRequestToken() {
        return requestToken;
    }

    public void setRequestToken(String requestToken) {
        this.requestToken = requestToken;
    }

    public String getTenantSlug() {
        return tenantSlug;
    }

    public void setTenantSlug(String tenantSlug) {
        this.tenantSlug = tenantSlug;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public void setLoginUrl(String loginUrl) {
        this.loginUrl = loginUrl;
    }

    public OffsetDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(OffsetDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public String getTenantDatabase() {
        return tenantDatabase;
    }

    public void setTenantDatabase(String tenantDatabase) {
        this.tenantDatabase = tenantDatabase;
    }

    public String getProvisioningError() {
        return provisioningError;
    }

    public void setProvisioningError(String provisioningError) {
        this.provisioningError = provisioningError;
    }

    public Boolean getExistingDemo() {
        return existingDemo;
    }

    public void setExistingDemo(Boolean existingDemo) {
        this.existingDemo = existingDemo;
    }

    public Boolean getAccessEmailSent() {
        return accessEmailSent;
    }

    public void setAccessEmailSent(Boolean accessEmailSent) {
        this.accessEmailSent = accessEmailSent;
    }

    public String getEmailError() {
        return emailError;
    }

    public void setEmailError(String emailError) {
        this.emailError = emailError;
    }

    public Boolean getShowTemporaryPassword() {
        return showTemporaryPassword;
    }

    public void setShowTemporaryPassword(Boolean showTemporaryPassword) {
        this.showTemporaryPassword = showTemporaryPassword;
    }
}
