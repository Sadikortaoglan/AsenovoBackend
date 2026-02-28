package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.data.TenantDescriptor;

public final class TenantContext {

    private static final ThreadLocal<TenantDescriptor> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setCurrentTenant(TenantDescriptor tenantDescriptor) {
        CURRENT_TENANT.set(tenantDescriptor);
    }

    public static TenantDescriptor getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    public static Long getTenantId(){
        TenantDescriptor tenant = getCurrentTenant();
        return tenant != null ? tenant.getId() : null;
    }

    public static boolean hasTenant() {
        return CURRENT_TENANT.get() != null;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
