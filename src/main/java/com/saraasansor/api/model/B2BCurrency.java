package com.saraasansor.api.model;

public enum B2BCurrency {
    TRY("Turkish Lira"),
    USD("US Dollar"),
    EUR("Euro"),
    GBP("British Pound");

    private final String displayName;

    B2BCurrency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
