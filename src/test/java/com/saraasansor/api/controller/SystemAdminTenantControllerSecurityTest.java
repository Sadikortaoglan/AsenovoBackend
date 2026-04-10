package com.saraasansor.api.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class SystemAdminTenantControllerSecurityTest {

    @Test
    void controllerShouldRequirePlatformAdminRole() {
        PreAuthorize preAuthorize = SystemAdminTenantController.class.getAnnotation(PreAuthorize.class);
        assertThat(preAuthorize).isNotNull();
        assertThat(preAuthorize.value()).isEqualTo("hasRole('PLATFORM_ADMIN')");
    }
}
