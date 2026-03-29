package com.saraasansor.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAdminUserManagementControllerSecurityTest {

    @Test
    void controllerShouldRequirePlatformAdminRole() {
        PreAuthorize preAuthorize = SystemAdminUserManagementController.class.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('PLATFORM_ADMIN')");
    }

    @Test
    void crossTenantTenantUserEndpointsShouldBeDisabled() {
        Method getTenantUsersMethod = findMethod("getTenantUsers");
        Method createTenantUserMethod = findMethod("createTenantUser");
        Method resetTenantUserPasswordMethod = findMethod("resetTenantUserPassword");

        assertThat(getTenantUsersMethod.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(getTenantUsersMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("denyAll()");

        assertThat(createTenantUserMethod.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(createTenantUserMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("denyAll()");

        assertThat(resetTenantUserPasswordMethod.getAnnotation(PreAuthorize.class)).isNotNull();
        assertThat(resetTenantUserPasswordMethod.getAnnotation(PreAuthorize.class).value()).isEqualTo("denyAll()");
    }

    private Method findMethod(String methodName) {
        for (Method method : SystemAdminUserManagementController.class.getDeclaredMethods()) {
            if (method.getName().equals(methodName)) {
                return method;
            }
        }
        throw new AssertionError("Method not found: " + methodName);
    }
}
