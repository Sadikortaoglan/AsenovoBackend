package com.saraasansor.api.tenant.dto;

public class TenantBrandingUpdateRequestDTO {

    private String primaryColor;
    private String secondaryColor;

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
}
