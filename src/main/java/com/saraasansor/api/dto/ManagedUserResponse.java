package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.saraasansor.api.model.User;

import java.time.LocalDateTime;

public class ManagedUserResponse {

    private Long id;
    private String username;
    private String role;
    private String userType;
    @JsonProperty("linkedB2BUnitId")
    private Long linkedB2bUnitId;
    @JsonProperty("linkedB2BUnitName")
    private String linkedB2bUnitName;
    private Long b2bUnitId;
    private Boolean active;
    private Boolean enabled;
    private Boolean locked;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ManagedUserResponse fromEntity(User user) {
        ManagedUserResponse response = new ManagedUserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setRole(user.getCanonicalRole() != null ? user.getCanonicalRole().name() : null);
        response.setUserType(user.getUserType() != null ? user.getUserType().name() : null);
        response.setLinkedB2bUnitId(user.getB2bUnit() != null ? user.getB2bUnit().getId() : null);
        response.setLinkedB2bUnitName(user.getB2bUnit() != null ? user.getB2bUnit().getName() : null);
        response.setB2bUnitId(user.getB2bUnit() != null ? user.getB2bUnit().getId() : null);
        response.setActive(user.getActive());
        response.setEnabled(user.getEnabled());
        response.setLocked(user.getLocked());
        response.setLastLoginAt(user.getLastLoginAt());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getUserType() {
        return userType;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    public Long getB2bUnitId() {
        return b2bUnitId;
    }

    public void setB2bUnitId(Long b2bUnitId) {
        this.b2bUnitId = b2bUnitId;
    }

    public Long getLinkedB2bUnitId() {
        return linkedB2bUnitId;
    }

    public void setLinkedB2bUnitId(Long linkedB2bUnitId) {
        this.linkedB2bUnitId = linkedB2bUnitId;
    }

    public String getLinkedB2bUnitName() {
        return linkedB2bUnitName;
    }

    public void setLinkedB2bUnitName(String linkedB2bUnitName) {
        this.linkedB2bUnitName = linkedB2bUnitName;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getLocked() {
        return locked;
    }

    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
