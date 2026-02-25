package com.saraasansor.api.dto;

public class StartMaintenancePlanRequest {
    /**
     * QR token from scanned QR code
     * Required for TECHNICIAN role, optional for ADMIN when remoteStart = true
     */
    private String qrToken;
    
    /**
     * Remote start flag
     * true = ADMIN can start without QR (remote start)
     * false = Normal start with QR validation
     */
    private Boolean remoteStart = false;
    
    public String getQrToken() {
        return qrToken;
    }
    
    public void setQrToken(String qrToken) {
        this.qrToken = qrToken;
    }
    
    public Boolean getRemoteStart() {
        return remoteStart != null ? remoteStart : false;
    }
    
    public void setRemoteStart(Boolean remoteStart) {
        this.remoteStart = remoteStart;
    }
}
