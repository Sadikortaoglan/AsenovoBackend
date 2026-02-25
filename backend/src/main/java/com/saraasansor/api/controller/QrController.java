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
    
    @Autowired
    private com.saraasansor.api.service.ElevatorQrService elevatorQrService;
    
    @Autowired
    private com.saraasansor.api.service.QrSessionService qrSessionService;
    
    @Autowired
    private com.saraasansor.api.repository.UserRepository userRepository;
    
    @Autowired
    private com.saraasansor.api.repository.ElevatorRepository elevatorRepository;
    
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
    
    /**
     * GET /api/qr/validate?e={elevatorCode}&s={signature}
     * Validates QR code signature when scanning
     * Returns elevator information if valid
     */
    @GetMapping("/validate")
    public ResponseEntity<ApiResponse<QrValidationResponse>> validateQr(
            @RequestParam("e") String elevatorCode,
            @RequestParam("s") String signature) {
        try {
            // Validate signature
            boolean isValid = elevatorQrService.validateSignature(elevatorCode, signature);
            
            if (!isValid) {
                return ResponseEntity.status(403)
                        .body(ApiResponse.error("Invalid QR signature"));
            }
            
            // Get elevator
            com.saraasansor.api.model.Elevator elevator = elevatorQrService.getElevatorByCode(elevatorCode);
            
            QrValidationResponse response = new QrValidationResponse();
            response.setValid(true);
            response.setElevatorId(elevator.getId());
            response.setElevatorCode(elevatorCode);
            response.setBuildingName(elevator.getBuildingName());
            response.setAddress(elevator.getAddress());
            
            return ResponseEntity.ok(ApiResponse.success("QR code is valid", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * POST /api/qr/validate
     * Validates QR code and creates session token for maintenance creation
     * 
     * Request body:
     * {
     *   "qrCode": "e=ELEV-001&s=signature" or full URL,
     *   "elevatorId": 123 (optional, will be resolved from QR if not provided)
     * }
     * 
     * Returns:
     * {
     *   "success": true,
     *   "data": {
     *     "qrSessionToken": "...",
     *     "elevatorId": 123,
     *     "expiresAt": "...",
     *     "startedRemotely": false
     *   }
     * }
     */
    @PostMapping("/validate")
    public ResponseEntity<ApiResponse<com.saraasansor.api.service.QrSessionService.QrSessionTokenResponse>> validateQrAndCreateSession(
            @RequestBody QrValidateRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            // Get current user
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("User not authenticated"));
            }
            
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            com.saraasansor.api.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Parse QR code
            String qrCode = request.getQrCode();
            if (qrCode == null || qrCode.trim().isEmpty()) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error("QR code is required"));
            }
            
            // Extract elevator code and signature from QR
            String elevatorCode = null;
            String signature = null;
            
            if (qrCode.contains("qr-start") || qrCode.contains("?")) {
                String queryString = qrCode.contains("?") 
                    ? qrCode.substring(qrCode.indexOf("?") + 1) 
                    : qrCode;
                String[] parts = queryString.split("&");
                for (String part : parts) {
                    if (part.startsWith("e=")) {
                        elevatorCode = part.substring(2);
                    } else if (part.startsWith("s=")) {
                        signature = part.substring(2);
                    }
                }
            } else {
                String[] parts = qrCode.split("&");
                for (String part : parts) {
                    if (part.startsWith("e=")) {
                        elevatorCode = part.substring(2);
                    } else if (part.startsWith("s=")) {
                        signature = part.substring(2);
                    }
                }
            }
            
            if (elevatorCode == null || signature == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error("Invalid QR code format"));
            }
            
            // Validate signature
            boolean isValid = elevatorQrService.validateSignature(elevatorCode, signature);
            if (!isValid) {
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("Invalid QR signature"));
            }
            
            // Get elevator
            com.saraasansor.api.model.Elevator elevator = elevatorQrService.getElevatorByCode(elevatorCode);
            Long elevatorId = elevator.getId();
            
            // Verify elevator match if provided
            if (request.getElevatorId() != null && !request.getElevatorId().equals(elevatorId)) {
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("QR code does not match elevator"));
            }
            
            // Get IP address
            String ipAddress = httpRequest.getRemoteAddr();
            String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                ipAddress = xForwardedFor.split(",")[0].trim();
            }
            
            // Create session token
            com.saraasansor.api.service.QrSessionService.QrSessionTokenResponse tokenResponse = 
                qrSessionService.createSessionToken(
                    currentUser.getId(),
                    currentUser.getRole(),
                    elevatorId,
                    false, // Not remote start
                    ipAddress
                );
            
            return ResponseEntity.ok(ApiResponse.success("QR validated and session token created", tokenResponse));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    /**
     * POST /api/qr/remote-start
     * Creates session token for ADMIN remote start (no QR required)
     * 
     * Request body:
     * {
     *   "elevatorId": 123
     * }
     * 
     * Returns:
     * {
     *   "success": true,
     *   "data": {
     *     "qrSessionToken": "...",
     *     "elevatorId": 123,
     *     "expiresAt": "...",
     *     "startedRemotely": true
     *   }
     * }
     */
    @PostMapping("/remote-start")
    public ResponseEntity<ApiResponse<com.saraasansor.api.service.QrSessionService.QrSessionTokenResponse>> remoteStart(
            @RequestBody RemoteStartRequest request,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            // Get current user
            org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !(authentication.getPrincipal() instanceof org.springframework.security.core.userdetails.UserDetails)) {
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("User not authenticated"));
            }
            
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                (org.springframework.security.core.userdetails.UserDetails) authentication.getPrincipal();
            com.saraasansor.api.model.User currentUser = userRepository.findByUsername(userDetails.getUsername())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            // Only ADMIN allowed
            if (currentUser.getRole() != com.saraasansor.api.model.User.Role.ADMIN && 
                currentUser.getRole() != com.saraasansor.api.model.User.Role.PATRON) {
                return ResponseEntity.status(403)
                    .body(ApiResponse.error("Remote start is only allowed for ADMIN role"));
            }
            
            // Validate elevator exists
            Long elevatorId = request.getElevatorId();
            if (elevatorId == null) {
                return ResponseEntity.status(400)
                    .body(ApiResponse.error("Elevator ID is required"));
            }
            
            if (!elevatorRepository.existsById(elevatorId)) {
                return ResponseEntity.status(404)
                    .body(ApiResponse.error("Elevator not found"));
            }
            
            // Get IP address
            String ipAddress = httpRequest.getRemoteAddr();
            String xForwardedFor = httpRequest.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                ipAddress = xForwardedFor.split(",")[0].trim();
            }
            
            // Create session token with remote start flag
            com.saraasansor.api.service.QrSessionService.QrSessionTokenResponse tokenResponse = 
                qrSessionService.createSessionToken(
                    currentUser.getId(),
                    currentUser.getRole(),
                    elevatorId,
                    true, // Remote start
                    ipAddress
                );
            
            return ResponseEntity.ok(ApiResponse.success("Remote start session token created", tokenResponse));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    public static class QrValidateRequest {
        private String qrCode;
        private Long elevatorId;
        
        public String getQrCode() { return qrCode; }
        public void setQrCode(String qrCode) { this.qrCode = qrCode; }
        public Long getElevatorId() { return elevatorId; }
        public void setElevatorId(Long elevatorId) { this.elevatorId = elevatorId; }
    }
    
    public static class RemoteStartRequest {
        private Long elevatorId;
        
        public Long getElevatorId() { return elevatorId; }
        public void setElevatorId(Long elevatorId) { this.elevatorId = elevatorId; }
    }
    
    public static class QrValidationResponse {
        private Boolean valid;
        private Long elevatorId;
        private String elevatorCode;
        private String buildingName;
        private String address;
        
        public Boolean getValid() {
            return valid;
        }
        
        public void setValid(Boolean valid) {
            this.valid = valid;
        }
        
        public Long getElevatorId() {
            return elevatorId;
        }
        
        public void setElevatorId(Long elevatorId) {
            this.elevatorId = elevatorId;
        }
        
        public String getElevatorCode() {
            return elevatorCode;
        }
        
        public void setElevatorCode(String elevatorCode) {
            this.elevatorCode = elevatorCode;
        }
        
        public String getBuildingName() {
            return buildingName;
        }
        
        public void setBuildingName(String buildingName) {
            this.buildingName = buildingName;
        }
        
        public String getAddress() {
            return address;
        }
        
        public void setAddress(String address) {
            this.address = address;
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
