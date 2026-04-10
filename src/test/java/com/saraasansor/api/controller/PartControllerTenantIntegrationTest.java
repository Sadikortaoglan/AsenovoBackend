package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.dto.PartCreateRequest;
import com.saraasansor.api.dto.PartPageResponse;
import com.saraasansor.api.dto.PartResponse;
import com.saraasansor.api.service.PartService;
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

class PartControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void partLookupShouldWorkWithResolvedTenantContext() throws Exception {
        PartService partService = mock(PartService.class);
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

        when(partService.getLookup("motor")).thenReturn(List.of(new LookupDto(2L, "Motor")));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PartController(partService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/parts/lookup")
                        .with(host("acme.example.com"))
                        .param("query", "motor"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Motor"));

        verify(partService, times(1)).getLookup("motor");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void partCrudShouldReturnExpectedPayload() throws Exception {
        PartService partService = mock(PartService.class);
        PartResponse response = new PartResponse();
        response.setId(10L);
        response.setName("Motor");
        response.setCode("MTR-001");
        response.setSalePrice(150.0);
        response.setActive(true);
        when(partService.createPart(any(PartCreateRequest.class))).thenReturn(response);
        when(partService.getParts(any(), any(), any())).thenReturn(new PartPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new PartController(partService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/parts")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload(
                "Motor", "MTR-001", "869000000001", 9L, 3L, 4L, 5L, 6L, 100.0, 150.0, "Test", null, 10, 12, 2, true
        ));
        mockMvc.perform(post("/parts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.code").value("MTR-001"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String name,
                                 String code,
                                 String barcode,
                                 Long vatRateId,
                                 Long stockGroupId,
                                 Long unitId,
                                 Long brandId,
                                 Long modelId,
                                 Double purchasePrice,
                                 Double salePrice,
                                 String description,
                                 String imagePath,
                                 Integer stock,
                                 Integer stockEntry,
                                 Integer stockExit,
                                 Boolean active) {
    }
}
