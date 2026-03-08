package com.saraasansor.api.service;

import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * QR Session Token Service
 * Manages short-lived tokens for maintenance creation QR validation.
 * 
 * TODO: Production için Redis veya DB tabanlı session storage önerilir.
 * Note: In-memory storage for development. Production should use Redis or database.
 */
@Service
public class QrSessionService {
    
    private static final Logger logger = LoggerFactory.getLogger(QrSessionService.class);
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private ElevatorQrService elevatorQrService;
    
    @Value("${app.qr.session-token-ttl-minutes:5}")
    private int sessionTokenTtlMinutes = 5;
    
    // In-memory token storage (dev only - use Redis/DB in production)
    private final Map<String, QrSessionToken> tokenStore = new ConcurrentHashMap<>();
    
    /**
     * Create QR session token after QR validation
     * 
     * @param userId User ID
     * @param userRole User role
     * @param elevatorId Elevator ID
     * @param startedRemotely Whether this is a remote start (ADMIN bypass)
     * @param ipAddress IP address (optional)
     * @return Session token
     */
    public QrSessionTokenResponse createSessionToken(
            Long userId, 
            User.Role userRole, 
            Long elevatorId, 
            boolean startedRemotely,
            String ipAddress) {
        
        // Generate token
        String token = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(sessionTokenTtlMinutes);
        
        // Create session token object
        QrSessionToken sessionToken = new QrSessionToken();
        sessionToken.setToken(token);
        sessionToken.setUserId(userId);
        sessionToken.setUserRole(userRole);
        sessionToken.setElevatorId(elevatorId);
        sessionToken.setStartedRemotely(startedRemotely);
        sessionToken.setCreatedAt(LocalDateTime.now());
        sessionToken.setExpiresAt(expiresAt);
        sessionToken.setIpAddress(ipAddress);
        
        // Store token
        tokenStore.put(token, sessionToken);
        
        // Cleanup expired tokens (simple cleanup - production should use scheduled task)
        cleanupExpiredTokens();
        
        // Log audit (in-memory for now - production should use database)
        logMaintenanceStart(userId, userRole, elevatorId, startedRemotely, ipAddress);
        
        // Return response
        QrSessionTokenResponse response = new QrSessionTokenResponse();
        response.setQrSessionToken(token);
        response.setElevatorId(elevatorId);
        response.setExpiresAt(expiresAt);
        response.setStartedRemotely(startedRemotely);
        
        return response;
    }
    
    /**
     * Validate QR session token
     * 
     * @param token Session token
     * @param userId Expected user ID
     * @param elevatorId Expected elevator ID
     * @return Validation result
     */
    public TokenValidationResult validateToken(String token, Long userId, Long elevatorId) {
        if (token == null || token.trim().isEmpty()) {
            return TokenValidationResult.invalid("QR session token is required");
        }
        
        QrSessionToken sessionToken = tokenStore.get(token);
        
        if (sessionToken == null) {
            return TokenValidationResult.invalid("Invalid QR session token");
        }
        
        // Check expiration
        if (sessionToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            tokenStore.remove(token); // Cleanup
            return TokenValidationResult.invalid("QR session token has expired");
        }
        
        // Verify user match
        if (!sessionToken.getUserId().equals(userId)) {
            return TokenValidationResult.invalid("QR session token does not match user");
        }
        
        // Verify elevator match
        if (!sessionToken.getElevatorId().equals(elevatorId)) {
            return TokenValidationResult.invalid("QR session token does not match elevator");
        }
        
        // Token is valid
        return TokenValidationResult.valid(sessionToken.isStartedRemotely());
    }
    
    /**
     * Invalidate token (mark as used)
     */
    public void invalidateToken(String token) {
        tokenStore.remove(token);
    }
    
    /**
     * Cleanup expired tokens
     */
    private void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        tokenStore.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }
    
    /**
     * Log maintenance start (audit)
     */
    private void logMaintenanceStart(Long userId, User.Role userRole, Long elevatorId, 
                                     boolean startedRemotely, String ipAddress) {
        logger.info("Maintenance start - UserId: {}, Role: {}, ElevatorId: {}, Remote: {}, IP: {}", 
            userId, userRole, elevatorId, startedRemotely, ipAddress);
    }
    
    /**
     * QR Session Token (internal)
     */
    public static class QrSessionToken {
        private String token;
        private Long userId;
        private User.Role userRole;
        private Long elevatorId;
        private boolean startedRemotely;
        private LocalDateTime createdAt;
        private LocalDateTime expiresAt;
        private String ipAddress;
        
        // Getters and Setters
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public User.Role getUserRole() { return userRole; }
        public void setUserRole(User.Role userRole) { this.userRole = userRole; }
        public Long getElevatorId() { return elevatorId; }
        public void setElevatorId(Long elevatorId) { this.elevatorId = elevatorId; }
        public boolean isStartedRemotely() { return startedRemotely; }
        public void setStartedRemotely(boolean startedRemotely) { this.startedRemotely = startedRemotely; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    }
    
    /**
     * Token Validation Result
     */
    public static class TokenValidationResult {
        private boolean valid;
        private boolean startedRemotely;
        private String error;
        
        private TokenValidationResult(boolean valid, boolean startedRemotely, String error) {
            this.valid = valid;
            this.startedRemotely = startedRemotely;
            this.error = error;
        }
        
        public static TokenValidationResult valid(boolean startedRemotely) {
            return new TokenValidationResult(true, startedRemotely, null);
        }
        
        public static TokenValidationResult invalid(String error) {
            return new TokenValidationResult(false, false, error);
        }
        
        public boolean isValid() { return valid; }
        public boolean isStartedRemotely() { return startedRemotely; }
        public String getError() { return error; }
    }
    
    /**
     * Session Token Response
     */
    public static class QrSessionTokenResponse {
        private String qrSessionToken;
        private Long elevatorId;
        private LocalDateTime expiresAt;
        private boolean startedRemotely;
        private String intent;
        
        public String getQrSessionToken() { return qrSessionToken; }
        public void setQrSessionToken(String qrSessionToken) { this.qrSessionToken = qrSessionToken; }
        public Long getElevatorId() { return elevatorId; }
        public void setElevatorId(Long elevatorId) { this.elevatorId = elevatorId; }
        public LocalDateTime getExpiresAt() { return expiresAt; }
        public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
        public boolean isStartedRemotely() { return startedRemotely; }
        public void setStartedRemotely(boolean startedRemotely) { this.startedRemotely = startedRemotely; }
        public String getIntent() { return intent; }
        public void setIntent(String intent) { this.intent = intent; }
    }
}
