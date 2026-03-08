package com.saraasansor.api.controller;

import com.saraasansor.api.dto.ApiResponse;
import com.saraasansor.api.dto.UserRequestDto;
import com.saraasansor.api.model.B2BUnit;
import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.B2BUnitRepository;
import com.saraasansor.api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@PreAuthorize("hasRole('TENANT_ADMIN')")
public class UserController {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private B2BUnitRepository b2bUnitRepository;
    
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

            user.setUserType(resolveUserType(dto.getRole(), dto.getUserType()));
            user.setStaffId(dto.getStaffId());
            user.setB2bUnit(resolveB2BUnit(dto.getB2bUnitId(), dto.getRole()));
            user.setActive(dto.getActive() != null ? dto.getActive() : true);
            user.setEnabled(dto.getActive() != null ? dto.getActive() : true);
            user.setLocked(false);
            
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
                if (userRepository.existsByUsernameAndIdNot(dto.getUsername(), id)) {
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
            // CRITICAL: Prevent changing role from SYSTEM_ADMIN to other if this is the last active SYSTEM_ADMIN
            if (dto.getRole() != null && dto.getRole() != userToUpdate.getRole()) {
                if (userToUpdate.getRole() == User.Role.SYSTEM_ADMIN && userToUpdate.getActive()) {
                    long activeSystemAdminCount = userRepository.countActiveUsersByRole(User.Role.SYSTEM_ADMIN);
                    if (activeSystemAdminCount <= 1) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("En az bir aktif SYSTEM_ADMIN bulunmalıdır."));
                    }
                }
                userToUpdate.setRole(dto.getRole());
                userToUpdate.setUserType(resolveUserType(dto.getRole(), dto.getUserType()));
            }

            if (dto.getStaffId() != null) {
                userToUpdate.setStaffId(dto.getStaffId());
            }

            if (dto.getRole() != null || dto.getB2bUnitId() != null) {
                User.Role effectiveRole = dto.getRole() != null ? dto.getRole() : userToUpdate.getRole();
                userToUpdate.setB2bUnit(resolveB2BUnit(dto.getB2bUnitId(), effectiveRole));
            }
            
            // Update active status if provided
            // CRITICAL: Prevent deactivating the last active SYSTEM_ADMIN
            if (dto.getActive() != null && !dto.getActive()) {
                if (userToUpdate.getRole() == User.Role.SYSTEM_ADMIN && userToUpdate.getActive()) {
                    long activeSystemAdminCount = userRepository.countActiveUsersByRole(User.Role.SYSTEM_ADMIN);
                    if (activeSystemAdminCount <= 1) {
                        return ResponseEntity.badRequest()
                                .body(ApiResponse.error("En az bir aktif SYSTEM_ADMIN bulunmalıdır."));
                    }
                }
                userToUpdate.setActive(false);
                userToUpdate.setEnabled(false);
            } else if (dto.getActive() != null && dto.getActive()) {
                userToUpdate.setActive(true);
                userToUpdate.setEnabled(true);
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
            
            // CRITICAL: Prevent deleting/deactivating the last active SYSTEM_ADMIN
            if (userToDelete.getRole() == User.Role.SYSTEM_ADMIN && userToDelete.getActive()) {
                long activeSystemAdminCount = userRepository.countActiveUsersByRole(User.Role.SYSTEM_ADMIN);
                if (activeSystemAdminCount <= 1) {
                    return ResponseEntity.badRequest()
                            .body(ApiResponse.error("En az bir aktif SYSTEM_ADMIN bulunmalıdır."));
                }
            }
            
            // SOFT DELETE: Set active = false (physical deletion forbidden)
            userToDelete.setActive(false);
            userToDelete.setEnabled(false);
            User saved = userRepository.save(userToDelete);
            
            // Don't return password hash
            saved.setPasswordHash(null);
            return ResponseEntity.ok(ApiResponse.success("User successfully deactivated (soft delete)", saved));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    private User.UserType resolveUserType(User.Role role, User.UserType requestedUserType) {
        if (requestedUserType != null) {
            return requestedUserType;
        }

        if (role == User.Role.SYSTEM_ADMIN) {
            return User.UserType.SYSTEM_ADMIN;
        }
        if (role == User.Role.CARI_USER) {
            return User.UserType.CARI;
        }
        return User.UserType.STAFF;
    }

    private B2BUnit resolveB2BUnit(Long b2bUnitId, User.Role role) {
        if (role == User.Role.CARI_USER) {
            if (b2bUnitId == null) {
                throw new RuntimeException("b2bUnitId is required for CARI_USER");
            }
            return b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                    .orElseThrow(() -> new RuntimeException("B2B unit not found"));
        }

        if (b2bUnitId == null) {
            return null;
        }

        return b2bUnitRepository.findByIdAndActiveTrue(b2bUnitId)
                .orElseThrow(() -> new RuntimeException("B2B unit not found"));
    }
}
