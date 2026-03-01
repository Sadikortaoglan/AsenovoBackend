package com.saraasansor.api.tenant.dto;

import java.time.LocalDateTime;

public class TenantBrandingResponseDTO {

    private Long id;
    private String name;
    private String logoUrl;
    private String primaryColor;
    private String secondaryColor;
    private LocalDateTime brandingUpdatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public LocalDateTime getBrandingUpdatedAt() {
        return brandingUpdatedAt;
    }

    public void setBrandingUpdatedAt(LocalDateTime brandingUpdatedAt) {
        this.brandingUpdatedAt = brandingUpdatedAt;
    }
}
