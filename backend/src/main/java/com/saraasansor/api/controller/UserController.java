package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.UserRequestDto;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @GetMapping
    public ResponseEntity<ApiResponse<List<User>>> getAllUsers() {
        List<User> users = userRepository.findAll(Sort.by(Sort.Direction.ASC, "id"));
        return ResponseEntity.ok(ApiResponse.success(users));
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> getUserById(@PathVariable Long id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return ResponseEntity.ok(ApiResponse.success(user.get()));
        } else {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("User not found"));
        }
    }
    
    @PostMapping
    public ResponseEntity<ApiResponse<User>> createUser(@Valid @RequestBody UserRequestDto dto) {
        try {
            if (dto.getPassword() == null || dto.getPassword().trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Password is required when creating a new user"));
            }
            
            if (userRepository.existsByUsername(dto.getUsername())) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Username already exists"));
            }
            
            User user = new User();
            user.setUsername(dto.getUsername());
            user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            
            if (dto.getRole() != null) {
                user.setRole(dto.getRole());
            } else {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Role is required"));
            }
            
            user.setActive(dto.getActive() != null ? dto.getActive() : true);
            
            User saved = userRepository.save(user);
            // Don't return password hash
            saved.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("User successfully created", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> updateUser(
            @PathVariable Long id, @RequestBody UserRequestDto dto) {
        try {
            Optional<User> existingUserOpt = userRepository.findById(id);
            if (!existingUserOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            User userToUpdate = existingUserOpt.get();
            
            // Update username if provided and changed
            if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty() && 
                !dto.getUsername().equals(userToUpdate.getUsername())) {
                if (userRepository.existsByUsername(dto.getUsername())) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("Username already exists"));
                }
                userToUpdate.setUsername(dto.getUsername());
            }
            
            // Update password ONLY if provided and not blank
            // If password is null, empty, or blank → keep existing password
            if (dto.getPassword() != null && !dto.getPassword().trim().isEmpty()) {
                userToUpdate.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
            }
            // No else - password is optional, existing password is preserved
            
            // Update role if provided
            // CRITICAL: Prevent changing role from PATRON to other if this is the last active PATRON
            if (dto.getRole() != null && dto.getRole() != userToUpdate.getRole()) {
                // If changing from PATRON to another role
                if (userToUpdate.getRole() == User.Role.PATRON && userToUpdate.getActive()) {
                    long activePatronCount = userRepository.countActiveUsersByRole(User.Role.PATRON);
                    if (activePatronCount <= 1) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("En az bir aktif PATRON bulunmalıdır. Son aktif PATRON'un rolü değiştirilemez."));
                    }
                }
                userToUpdate.setRole(dto.getRole());
            }
            
            // Update active status if provided
            // CRITICAL: Prevent deactivating the last active PATRON
            if (dto.getActive() != null && !dto.getActive()) {
                // If trying to deactivate a PATRON user
                if (userToUpdate.getRole() == User.Role.PATRON && userToUpdate.getActive()) {
                    long activePatronCount = userRepository.countActiveUsersByRole(User.Role.PATRON);
                    if (activePatronCount <= 1) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("En az bir aktif PATRON bulunmalıdır."));
                    }
                }
                userToUpdate.setActive(false);
            } else if (dto.getActive() != null && dto.getActive()) {
                // Activating is always allowed
                userToUpdate.setActive(true);
            }
            
            User saved = userRepository.save(userToUpdate);
            // Don't return password hash
            saved.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("User successfully updated", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<User>> deleteUser(@PathVariable Long id) {
        try {
            Optional<User> userOpt = userRepository.findById(id);
            if (!userOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("User not found"));
            }
            
            User userToDelete = userOpt.get();
            
            // CRITICAL BUSINESS RULE: Physical deletion is FORBIDDEN
            // Delete operation must be SOFT DELETE (active = false)
            
            // CRITICAL: Prevent deleting/deactivating the last active PATRON
            if (userToDelete.getRole() == User.Role.PATRON && userToDelete.getActive()) {
                long activePatronCount = userRepository.countActiveUsersByRole(User.Role.PATRON);
                if (activePatronCount <= 1) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("En az bir aktif PATRON bulunmalıdır."));
                }
            }
            
            // SOFT DELETE: Set active = false (physical deletion forbidden)
            userToDelete.setActive(false);
            User saved = userRepository.save(userToDelete);
            
            // Don't return password hash
            saved.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("User successfully deactivated (soft delete)", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }
}
