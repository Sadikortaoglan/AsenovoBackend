package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.service.QrProofService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/qr")
public class QrController {
    
    @Autowired
    private QrProofService qrProofService;
    
    @PostMapping("/issue-session-token")
    public ResponseEntity<ApiResponse<QrProofService.QrTokenResponse>> issueSessionToken(
            @RequestBody QrTokenRequest request,
            HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest.getRemoteAddr();
            QrProofService.QrTokenResponse response = qrProofService.issueSessionToken(
                    request.getElevatorPublicCode(),
                    request.getSig(),
                    request.getDeviceMeta(),
                    ip);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    public static class QrTokenRequest {
        private String elevatorPublicCode;
        private String sig;
        private String deviceMeta;
        
        public String getElevatorPublicCode() {
            return elevatorPublicCode;
        }
        
        public void setElevatorPublicCode(String elevatorPublicCode) {
            this.elevatorPublicCode = elevatorPublicCode;
        }
        
        public String getSig() {
            return sig;
        }
        
        public void setSig(String sig) {
            this.sig = sig;
        }
        
        public String getDeviceMeta() {
            return deviceMeta;
        }
        
        public void setDeviceMeta(String deviceMeta) {
            this.deviceMeta = deviceMeta;
        }
    }
}
