package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.auth.LoginRequest;
import com.saraasansor.api.dto.auth.LoginResponse;
import com.saraasansor.api.dto.auth.RefreshTokenRequest;
import com.saraasansor.api.dto.auth.RegisterRequest;
import com.saraasansor.api.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
    
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }
    
    @PostMapping(value = {"/login", "/api/login"}, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Login successful", response));
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Invalid username or password"));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Login failed: " + e.getMessage()));
        }
    }
    
    @PostMapping(value = {"/refresh", "/api/refresh"}, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        try {
            LoginResponse response = authService.refreshToken(request.getRefreshToken());
            return ResponseEntity.ok(ApiResponse.success("Token refreshed", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("Token refresh failed: " + e.getMessage()));
        }
    }
    
    @PostMapping(value = {"/register", "/api/register"}, consumes = "application/json", produces = "application/json")
    public ResponseEntity<ApiResponse<LoginResponse>> register(@Valid @RequestBody RegisterRequest request) {
        try {
            LoginResponse response = authService.register(request);
            return ResponseEntity.ok(ApiResponse.success("User successfully registered and logged in", response));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Registration failed: " + e.getMessage()));
        }
    }
    
    @PostMapping({"/logout", "/api/logout"})
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) com.saraasansor.api.dto.auth.RefreshTokenRequest request) {
        try {
            if (request != null && request.getRefreshToken() != null) {
                authService.logout(request.getRefreshToken());
            } else {
                authService.logout();
            }
            return ResponseEntity.ok(ApiResponse.success("Logout successful", null));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Logout failed: " + e.getMessage()));
        }
    }
}
