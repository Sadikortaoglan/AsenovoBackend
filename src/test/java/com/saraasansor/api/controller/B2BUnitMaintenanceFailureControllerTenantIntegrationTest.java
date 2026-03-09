package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.B2BUnitMaintenanceFailureListItemResponse;
import com.saraasansor.api.service.B2BUnitMaintenanceFailureService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitMaintenanceFailureControllerTenantIntegrationTest {

    @Test
    void listEndpointShouldUseResolvedTenantContext() throws Exception {
        B2BUnitMaintenanceFailureService service = mock(B2BUnitMaintenanceFailureService.class);
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);

        when(tenantRegistryService.findActiveBySubdomain("acme"))
                .thenReturn(Optional.of(new TenantDescriptor(
                        1L,
                        "Acme",
                        "acme",
                        Tenant.TenancyMode.SHARED_SCHEMA,
                        "tenant_acme",
                        null,
                        null,
                        null,
                        null,
                        null,
                        "STARTER"
                )));

        B2BUnitMaintenanceFailureListItemResponse item = new B2BUnitMaintenanceFailureListItemResponse();
        item.setId(55L);
        item.setOperationDate(LocalDateTime.of(2026, 3, 9, 11, 30));
        item.setOperationType("MAINTENANCE");
        item.setSourceType("MAINTENANCE");
        item.setElevatorId(101L);
        item.setElevatorName("E-1");
        item.setFacilityId(11L);
        item.setFacilityName("Facility A");
        item.setStatus("COMPLETED");

        when(service.getCompletedMaintenanceFailures(eq(5L), eq("maint"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitMaintenanceFailureController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/5/maintenance-failures?search=maint").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(55))
                .andExpect(jsonPath("$.data.content[0].operationType").value("MAINTENANCE"));

        verify(service, times(1)).getCompletedMaintenanceFailures(eq(5L), eq("maint"), any(Pageable.class));
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void listEndpointShouldReturnForbiddenWhenServiceRejectsAccess() throws Exception {
        B2BUnitMaintenanceFailureService service = mock(B2BUnitMaintenanceFailureService.class);
        when(service.getCompletedMaintenanceFailures(eq(5L), eq(null), any(Pageable.class)))
                .thenThrow(new AccessDeniedException("Forbidden"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitMaintenanceFailureController(service))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/b2b-units/5/maintenance-failures"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }
}
