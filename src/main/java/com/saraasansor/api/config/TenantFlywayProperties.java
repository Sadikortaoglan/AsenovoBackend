package com.saraasansor.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.tenancy.migration")
public class TenantFlywayProperties {

    private boolean enabled = true;
    private boolean includeSharedSchemas = true;
    private boolean includeDedicatedDbs = true;
    private String defaultSharedSchema = "public";
    private String historyTable = "flyway_schema_history";
    private boolean baselineOnMigrate = true;
    private int connectRetries = 1;
    private List<String> locations = List.of("classpath:db/migration");

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isIncludeSharedSchemas() {
        return includeSharedSchemas;
    }

    public void setIncludeSharedSchemas(boolean includeSharedSchemas) {
        this.includeSharedSchemas = includeSharedSchemas;
    }

    public boolean isIncludeDedicatedDbs() {
        return includeDedicatedDbs;
    }

    public void setIncludeDedicatedDbs(boolean includeDedicatedDbs) {
        this.includeDedicatedDbs = includeDedicatedDbs;
    }

    public String getDefaultSharedSchema() {
        return defaultSharedSchema;
    }

    public void setDefaultSharedSchema(String defaultSharedSchema) {
        this.defaultSharedSchema = defaultSharedSchema;
    }

    public String getHistoryTable() {
        return historyTable;
    }

    public void setHistoryTable(String historyTable) {
        this.historyTable = historyTable;
    }

    public boolean isBaselineOnMigrate() {
        return baselineOnMigrate;
    }

    public void setBaselineOnMigrate(boolean baselineOnMigrate) {
        this.baselineOnMigrate = baselineOnMigrate;
    }

    public int getConnectRetries() {
        return connectRetries;
    }

    public void setConnectRetries(int connectRetries) {
        this.connectRetries = connectRetries;
    }

    public List<String> getLocations() {
        return locations;
    }

    public void setLocations(List<String> locations) {
        this.locations = locations;
    }
}

