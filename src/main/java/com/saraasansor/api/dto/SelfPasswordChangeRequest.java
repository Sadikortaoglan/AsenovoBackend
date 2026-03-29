package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotBlank;

public class SelfPasswordChangeRequest {

    @NotBlank(message = "currentPassword is required")
    private String currentPassword;

    @NotBlank(message = "newPassword is required")
    private String newPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String resolveCurrentPassword() {
        return currentPassword != null ? currentPassword.trim() : null;
    }

    public String resolveNewPassword() {
        return newPassword != null ? newPassword.trim() : null;
    }
}
