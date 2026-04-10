package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.ModelCreateRequest;
import com.saraasansor.api.dto.ModelLookupDto;
import com.saraasansor.api.dto.ModelPageResponse;
import com.saraasansor.api.dto.ModelResponse;
import com.saraasansor.api.service.ModelService;
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

class ModelControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void modelLookupShouldWorkWithResolvedTenantContext() throws Exception {
        ModelService modelService = mock(ModelService.class);
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

        ModelLookupDto lookup = new ModelLookupDto();
        lookup.setId(2L);
        lookup.setName("Gen2");
        lookup.setBrandId(3L);
        lookup.setBrandName("Otis");
        when(modelService.getLookup("gen")).thenReturn(List.of(lookup));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ModelController(modelService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/models/lookup")
                        .with(host("acme.example.com"))
                        .param("query", "gen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Gen2"))
                .andExpect(jsonPath("$.data[0].brandName").value("Otis"));

        verify(modelService, times(1)).getLookup("gen");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void modelCrudShouldReturnExpectedPayload() throws Exception {
        ModelService modelService = mock(ModelService.class);
        ModelResponse response = new ModelResponse();
        response.setId(10L);
        response.setName("MonoSpace");
        response.setBrandId(5L);
        response.setBrandName("Kone");
        response.setActive(true);
        when(modelService.createModel(any(ModelCreateRequest.class))).thenReturn(response);
        when(modelService.getModels(any(), any(), any())).thenReturn(new ModelPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new ModelController(modelService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/models")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("MonoSpace", 5L, true));
        mockMvc.perform(post("/models")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.brandName").value("Kone"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record CreatePayload(String name, Long brandId, Boolean active) {
    }
}
