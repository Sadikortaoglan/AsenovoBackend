package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.dto.WarehouseCreateRequest;
import com.saraasansor.api.dto.WarehousePageResponse;
import com.saraasansor.api.dto.WarehouseResponse;
import com.saraasansor.api.service.WarehouseService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WarehouseControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void warehouseLookupShouldWorkWithResolvedTenantContext() throws Exception {
        WarehouseService warehouseService = mock(WarehouseService.class);
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

        when(warehouseService.getLookup("ana")).thenReturn(List.of(new LookupDto(2L, "Ana Depo")));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new WarehouseController(warehouseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/warehouses/lookup")
                        .with(host("acme.example.com"))
                        .param("query", "ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Ana Depo"));

        verify(warehouseService, times(1)).getLookup("ana");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void warehouseCrudShouldReturnExpectedPayload() throws Exception {
        WarehouseService warehouseService = mock(WarehouseService.class);
        WarehouseResponse response = new WarehouseResponse();
        response.setId(10L);
        response.setName("Ana Depo");
        response.setActive(true);
        when(warehouseService.createWarehouse(any(WarehouseCreateRequest.class))).thenReturn(response);
        when(warehouseService.getWarehouses(any(), any(), any())).thenReturn(new WarehousePageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new WarehouseController(warehouseService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/warehouses")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("Ana Depo", true));
        mockMvc.perform(post("/warehouses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Ana Depo"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String name, Boolean active) {
    }
}
