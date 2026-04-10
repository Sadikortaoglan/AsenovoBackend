package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public class PlatformTenantUserResetPasswordRequest {

    @NotBlank(message = "newPassword is required")
    private String newPassword;

    @JsonProperty("password")
    private String password;

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String resolveNewPassword() {
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            return newPassword.trim();
        }
        if (password != null && !password.trim().isEmpty()) {
            return password.trim();
        }
        return null;
    }
}
