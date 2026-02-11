package com.saraasansor.api.dto;

import jakarta.validation.constraints.NotBlank;

public class StartMaintenancePlanRequest {
    @NotBlank(message = "QR token is required")
    private String qrToken;
    
    public String getQrToken() {
        return qrToken;
    }
    
    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
}
