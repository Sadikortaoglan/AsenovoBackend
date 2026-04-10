package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.StockGroupCreateRequest;
import com.saraasansor.api.dto.StockGroupPageResponse;
import com.saraasansor.api.dto.StockGroupResponse;
import com.saraasansor.api.service.StockGroupService;
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

class StockGroupControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void stockGroupListShouldWorkWithResolvedTenantContext() throws Exception {
        StockGroupService stockGroupService = mock(StockGroupService.class);
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

        StockGroupPageResponse response = new StockGroupPageResponse();
        when(stockGroupService.getStockGroups("ele", true, org.springframework.data.domain.PageRequest.of(0, 25)))
                .thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StockGroupController(stockGroupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/stock-groups")
                        .with(host("acme.example.com"))
                        .param("query", "ele"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(stockGroupService, times(1))
                .getStockGroups("ele", true, org.springframework.data.domain.PageRequest.of(0, 25));
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void stockGroupCrudShouldReturnExpectedPayload() throws Exception {
        StockGroupService stockGroupService = mock(StockGroupService.class);
        StockGroupResponse response = new StockGroupResponse();
        response.setId(10L);
        response.setName("Elektrik");
        response.setActive(true);
        when(stockGroupService.createStockGroup(any(StockGroupCreateRequest.class))).thenReturn(response);
        when(stockGroupService.getStockGroups(any(), any(), any())).thenReturn(new StockGroupPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new StockGroupController(stockGroupService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/stock-groups")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("Elektrik", true));
        mockMvc.perform(post("/stock-groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.name").value("Elektrik"));
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
