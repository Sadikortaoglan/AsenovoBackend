package com.saraasansor.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.saraasansor.api.model.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class TenantUserCreateRequest {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Role is required")
    private User.Role role;

    @JsonProperty("linkedB2BUnitId")
    private Long linkedB2bUnitId;
    private Long b2bUnitId;
    private Boolean enabled = true;
    private Boolean active = true;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public User.Role getRole() {
        return role;
    }

    public void setRole(User.Role role) {
        this.role = role;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long resolveLinkedB2bUnitId() {
        return linkedB2bUnitId != null ? linkedB2bUnitId : b2bUnitId;
    }

    public Boolean resolveEnabledValue() {
        if (enabled != null) {
            return enabled;
        }
        return active;
    }
}
