package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.data.TenantDescriptor;

public final class TenantCacheKeyBuilder {

    private TenantCacheKeyBuilder() {
    }

    /**
     * t:{tenantId}:{domain}:{key}
     */
    public static String buildKey(String domain, String key) {
        TenantDescriptor tenant = TenantContext.getCurrentTenant();

        String tenantPart = tenant != null && tenant.getId() != null
                ? String.valueOf(tenant.getId())
                : "global";

        String domainPart = domain != null ? domain : "default";
        String keyPart = key != null ? key : "";

        return "t:" + tenantPart + ":" + domainPart + ":" + keyPart;
    }
}

