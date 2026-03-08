package com.saraasansor.api.service;

import com.saraasansor.api.model.Elevator;
import com.saraasansor.api.model.QrProof;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.ElevatorRepository;
import com.saraasansor.api.repository.QrProofRepository;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
@Transactional
public class QrProofService {
    
    @Autowired
    private QrProofRepository qrProofRepository;
    
    @Autowired
    private ElevatorRepository elevatorRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Value("${app.qr.secret-key:default-secret-key-change-in-production}")
    private String qrSecretKey;
    
    @Value("${app.qr.token-ttl-minutes:3}")
    private int tokenTtlMinutes = 3;
    
    /**
     * Issue a short-lived session token after QR scan validation
     * 
     * @param elevatorPublicCode Public code from QR (e.g., "ELEV-002")
     * @param signature HMAC signature from QR code
     * @param deviceInfo Optional device metadata
     * @return Token string and expiration time
     */
    public QrTokenResponse issueSessionToken(String elevatorPublicCode, String signature, String deviceInfo, String ip) {
        // 1. Validate signature
        String expectedSignature = generateSignature(elevatorPublicCode);
        if (!expectedSignature.equals(signature)) {
            throw new RuntimeException("Invalid QR signature");
        }
        
        // 2. Find elevator by public code (identityNumber or elevatorNumber)
        Elevator elevator = elevatorRepository.findByIdentityNumber(elevatorPublicCode)
                .orElseGet(() -> elevatorRepository.findByElevatorNumber(elevatorPublicCode)
                        .orElseThrow(() -> new RuntimeException("Elevator not found: " + elevatorPublicCode)));
        
        // 3. Generate token
        String nonce = UUID.randomUUID().toString().replace("-", "");
        String token = generateToken(elevatorPublicCode, nonce);
        String tokenHash = hashToken(token);
        
        // 4. Create QrProof record
        QrProof qrProof = new QrProof();
        qrProof.setElevator(elevator);
        qrProof.setTokenHash(tokenHash);
        qrProof.setIssuedAt(LocalDateTime.now());
        qrProof.setExpiresAt(LocalDateTime.now().plusMinutes(tokenTtlMinutes));
        qrProof.setNonce(nonce);
        qrProof.setIp(ip);
        
        qrProofRepository.save(qrProof);
        
        // 5. Return token (only time it's returned in plain text)
        QrTokenResponse response = new QrTokenResponse();
        response.setQrToken(token);
        response.setExpiresAt(qrProof.getExpiresAt());
        response.setElevatorId(elevator.getId());
        
        return response;
    }
    
    /**
     * Validate and mark QR token as used when starting a session
     * 
     * @param qrToken Plain text token from frontend
     * @param technicianId Technician who is using the token
     * @return QrProof entity if valid
     */
    public QrProof validateAndUseToken(String qrToken, Long technicianId) {
        String tokenHash = hashToken(qrToken);
        LocalDateTime now = LocalDateTime.now();
        
        // Find valid, unused token
        QrProof qrProof = qrProofRepository.findValidToken(tokenHash, now)
                .orElseThrow(() -> new RuntimeException("Invalid or expired QR token"));
        
        // Mark as used
        qrProof.setUsedAt(now);
        User technician = userRepository.findById(technicianId)
                .orElseThrow(() -> new RuntimeException("Technician not found"));
        qrProof.setUsedBy(technician);
        
        qrProofRepository.save(qrProof);
        
        return qrProof;
    }
    
    /**
     * Generate HMAC signature for QR code
     */
    private String generateSignature(String elevatorPublicCode) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(qrSecretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] signatureBytes = mac.doFinal(elevatorPublicCode.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR signature", e);
        }
    }
    
    /**
     * Generate session token
     */
    private String generateToken(String elevatorPublicCode, String nonce) {
        String payload = elevatorPublicCode + ":" + nonce + ":" + System.currentTimeMillis();
        return Base64.getUrlEncoder().withoutPadding().encodeToString(payload.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Hash token for storage (SHA-256)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hashBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
    
    /**
     * Response DTO for token issue
     */
    public static class QrTokenResponse {
        private String qrToken;
        private LocalDateTime expiresAt;
        private Long elevatorId;
        
        public String getQrToken() {
            return qrToken;
        }
        
        public void setQrToken(String qrToken) {
            this.qrToken = qrToken;
        }
        
        public LocalDateTime getExpiresAt() {
            return expiresAt;
        }
        
        public void setExpiresAt(LocalDateTime expiresAt) {
            this.expiresAt = expiresAt;
        }
        
        public Long getElevatorId() {
            return elevatorId;
        }
        
        public void setElevatorId(Long elevatorId) {
            this.elevatorId = elevatorId;
        }
    }
}
