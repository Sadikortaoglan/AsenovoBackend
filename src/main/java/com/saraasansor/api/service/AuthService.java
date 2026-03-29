package com.saraasansor.api.service;

import com.saraasansor.api.dto.auth.LoginRequest;
import com.saraasansor.api.dto.auth.LoginResponse;
import com.saraasansor.api.dto.auth.RegisterRequest;
import com.saraasansor.api.model.RefreshToken;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.RefreshTokenRepository;
import com.saraasansor.api.repository.UserRepository;
import com.saraasansor.api.security.JwtTokenProvider;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private JwtTokenProvider tokenProvider;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RefreshTokenRepository refreshTokenRepository;
    
    private static final SecureRandom secureRandom = new SecureRandom();
    
    // Generate secure random refresh token (64+ characters)
    private String generateRefreshTokenValue() {
        byte[] randomBytes = new byte[48]; // 48 bytes = 64 base64 characters
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    // Hash refresh token for storage (SHA-256)
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
    
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );
        } catch (AuthenticationException ex) {
            throw ex;
        }
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // Generate access token (JWT, 15 minutes)
        String accessToken = tokenProvider.generateAccessToken(user);
        
        // Generate refresh token (UUID-like, 7 days)
        String refreshTokenValue = generateRefreshTokenValue();
        String hashedToken = hashToken(refreshTokenValue);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        
        // Store refresh token in database
        RefreshToken refreshToken = new RefreshToken(user, hashedToken, expiresAt);
        refreshTokenRepository.save(refreshToken);
        
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenValue); // Return raw token to client
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getCanonicalRole() != null ? user.getCanonicalRole().name() : null);
        response.setUserType(resolveUserTypeName(user));
        response.setB2bUnitId(user.getB2bUnit() != null ? user.getB2bUnit().getId() : null);
        applyAuthScope(response);
        
        return response;
    }
    
    @Transactional
    public LoginResponse refreshToken(String refreshTokenValue) {
        // Hash the provided token to compare with stored hash
        String hashedToken = hashToken(refreshTokenValue);
        
        // Find valid refresh token in database
        RefreshToken refreshToken = refreshTokenRepository
                .findValidToken(hashedToken, LocalDateTime.now())
                .orElseThrow(() -> new RuntimeException("Invalid or expired refresh token"));
        
        // Check if user is still active
        User user = refreshToken.getUser();
        if (!user.getActive()) {
            throw new RuntimeException("User account is not active");
        }
        
        // Generate new access token (15 minutes)
        String newAccessToken = tokenProvider.generateAccessToken(user);
        
        // Token rotation: Generate new refresh token and revoke old one
        String newRefreshTokenValue = generateRefreshTokenValue();
        String newHashedToken = hashToken(newRefreshTokenValue);
        LocalDateTime newExpiresAt = LocalDateTime.now().plusDays(7);
        
        // Revoke old token
        refreshToken.revoke();
        refreshTokenRepository.save(refreshToken);
        
        // Create and save new refresh token
        RefreshToken newRefreshToken = new RefreshToken(user, newHashedToken, newExpiresAt);
        refreshTokenRepository.save(newRefreshToken);
        
        LoginResponse response = new LoginResponse();
        response.setAccessToken(newAccessToken);
        response.setRefreshToken(newRefreshTokenValue); // Return new refresh token
        response.setUserId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getCanonicalRole() != null ? user.getCanonicalRole().name() : null);
        response.setUserType(resolveUserTypeName(user));
        response.setB2bUnitId(user.getB2bUnit() != null ? user.getB2bUnit().getId() : null);
        applyAuthScope(response);
        
        return response;
    }
    
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("This username is already in use");
        }
        
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        
        User.Role requestedRole;
        try {
            requestedRole = User.Role.fromExternalName(request.getRole());
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Invalid role. Must be TENANT_ADMIN, STAFF_USER or CARI_USER.");
        }

        User.Role canonicalRole = requestedRole.toCanonical();
        if (canonicalRole.isPlatformAdmin()) {
            throw new RuntimeException("PLATFORM_ADMIN cannot be created from public register endpoint.");
        }
        if (canonicalRole == User.Role.TENANT_ADMIN) {
            throw new RuntimeException("TENANT_ADMIN cannot be created from public register endpoint.");
        }

        user.setRole(canonicalRole.toPersistenceRole());
        user.setUserType(canonicalRole == User.Role.CARI_USER ? User.UserType.CARI : User.UserType.STAFF);
        
        user.setActive(true);
        user.setEnabled(true);
        user.setLocked(false);
        User savedUser = userRepository.save(user);
        
        // Auto login
        String accessToken = tokenProvider.generateAccessToken(savedUser);
        
        // Generate refresh token (UUID-like, 7 days)
        String refreshTokenValue = generateRefreshTokenValue();
        String hashedToken = hashToken(refreshTokenValue);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        
        // Store refresh token in database
        RefreshToken refreshToken = new RefreshToken(savedUser, hashedToken, expiresAt);
        refreshTokenRepository.save(refreshToken);
        
        LoginResponse response = new LoginResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshTokenValue); // Return raw token to client
        response.setUserId(savedUser.getId());
        response.setUsername(savedUser.getUsername());
        response.setRole(savedUser.getCanonicalRole() != null ? savedUser.getCanonicalRole().name() : null);
        response.setUserType(resolveUserTypeName(savedUser));
        response.setB2bUnitId(savedUser.getB2bUnit() != null ? savedUser.getB2bUnit().getId() : null);
        applyAuthScope(response);
        
        return response;
    }

    private String resolveUserTypeName(User user) {
        if (user.getUserType() != null) {
            return user.getUserType().name();
        }
        if (user.getRole() != null && user.getRole().isPlatformAdmin()) {
            return User.UserType.SYSTEM_ADMIN.name();
        }
        if (user.getRole() != null && user.getRole().isCariUser()) {
            return User.UserType.CARI.name();
        }
        return User.UserType.STAFF.name();
    }

    private void applyAuthScope(LoginResponse response) {
        TenantDescriptor tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            response.setAuthScopeType("PLATFORM");
            response.setTenantId(null);
            response.setTenantSchema(null);
            return;
        }

        response.setAuthScopeType("TENANT");
        response.setTenantId(tenant.getId());
        response.setTenantSchema(tenant.getSchemaName());
    }
    
    @Transactional
    public void logout() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String username = authentication.getName();
            User user = userRepository.findByUsername(username)
                    .orElse(null);
            if (user != null) {
                // Revoke all active refresh tokens for this user
                List<RefreshToken> activeTokens = refreshTokenRepository.findActiveTokensByUserId(user.getId());
                for (RefreshToken token : activeTokens) {
                    token.revoke();
                    refreshTokenRepository.save(token);
                }
            }
        }
        SecurityContextHolder.clearContext();
    }
    
    @Transactional
    public void logout(String refreshTokenValue) {
        // Hash the provided token
        String hashedToken = hashToken(refreshTokenValue);
        
        // Find and revoke the specific refresh token
        refreshTokenRepository.findByToken(hashedToken)
                .ifPresent(token -> {
                    token.revoke();
                    refreshTokenRepository.save(token);
                });
        
        SecurityContextHolder.clearContext();
    }
}
