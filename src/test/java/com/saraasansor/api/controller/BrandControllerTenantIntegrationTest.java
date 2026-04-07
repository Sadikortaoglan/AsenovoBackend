package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.BrandCreateRequest;
import com.saraasansor.api.dto.BrandLookupDto;
import com.saraasansor.api.dto.BrandPageResponse;
import com.saraasansor.api.dto.BrandResponse;
import com.saraasansor.api.service.BrandService;
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

class BrandControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void brandLookupShouldWorkWithResolvedTenantContext() throws Exception {
        BrandService brandService = mock(BrandService.class);
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

        BrandLookupDto lookup = new BrandLookupDto();
        lookup.setId(2L);
        lookup.setName("Otis");
        when(brandService.getLookup("ot")).thenReturn(List.of(lookup));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new BrandController(brandService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/brands/lookup")
                        .with(host("acme.example.com"))
                        .param("query", "ot"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Otis"));

        verify(brandService, times(1)).getLookup("ot");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void brandCrudShouldReturnExpectedPayload() throws Exception {
        BrandService brandService = mock(BrandService.class);
        BrandResponse response = new BrandResponse();
        response.setId(10L);
        response.setName("Schindler");
        response.setActive(true);
        when(brandService.createBrand(any(BrandCreateRequest.class))).thenReturn(response);
        when(brandService.getBrands(any(), any(), any())).thenReturn(new BrandPageResponse());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new BrandController(brandService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/brands")
                        .param("page", "0")
                        .param("size", "25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        String payload = OBJECT_MAPPER.writeValueAsString(new CreatePayload("Schindler", true));
        mockMvc.perform(post("/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(10));
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
