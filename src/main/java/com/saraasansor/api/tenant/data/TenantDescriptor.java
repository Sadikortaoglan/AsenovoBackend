package com.saraasansor.api.tenant.data;

import com.saraasansor.api.tenant.model.Tenant;

public class TenantDescriptor {

    private final Long id;
    private final String name;
    private final String subdomain;
    private final Tenant.TenancyMode tenancyMode;
    private final String schemaName;
    private final String dbHost;
    private final String dbName;
    private final String dbUsername;
    private final String dbPassword;
    private final String redisNamespace;
    private final String planType;

    public TenantDescriptor(Long id,
                            String name,
                            String subdomain,
                            Tenant.TenancyMode tenancyMode,
                            String schemaName,
                            String dbHost,
                            String dbName,
                            String dbUsername,
                            String dbPassword,
                            String redisNamespace,
                            String planType) {
        this.id = id;
        this.name = name;
        this.subdomain = subdomain;
        this.tenancyMode = tenancyMode;
        this.schemaName = schemaName;
        this.dbHost = dbHost;
        this.dbName = dbName;
        this.dbUsername = dbUsername;
        this.dbPassword = dbPassword;
        this.redisNamespace = redisNamespace;
        this.planType = planType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public Tenant.TenancyMode getTenancyMode() {
        return tenancyMode;
    }

    public String getSchemaName() {
        return schemaName;
    }

    public String getDbHost() {
        return dbHost;
    }

    public String getDbName() {
        return dbName;
    }

    public String getDbUsername() {
        return dbUsername;
    }

    public String getDbPassword() {
        return dbPassword;
    }

    public String getRedisNamespace() {
        return redisNamespace;
    }

    public String getPlanType() {
        return planType;
    }
}

