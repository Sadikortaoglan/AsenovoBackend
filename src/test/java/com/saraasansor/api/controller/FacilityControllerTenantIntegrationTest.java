package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.FacilityAddressDto;
import com.saraasansor.api.dto.FacilityDto;
import com.saraasansor.api.service.FacilityService;
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

class FacilityControllerTenantIntegrationTest {

    @Test
    void endpointShouldWorkWithResolvedTenantContext() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);
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

        FacilityDto dto = new FacilityDto();
        dto.setId(1L);
        dto.setName("Facility A");
        when(facilityService.getFacilities(eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/facilities").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(facilityService, times(1)).getFacilities(eq(null), eq(null), eq(null), any(Pageable.class));
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void endpointShouldReturnNotFoundForUnknownTenant() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);
        when(tenantRegistryService.findActiveBySubdomain("unknown")).thenReturn(Optional.empty());

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/facilities").with(host("unknown.example.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("TENANT_NOT_FOUND"));
    }

    @Test
    void endpointShouldReturnForbiddenWhenObjectAccessDenied() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);
        when(facilityService.getFacilityById(55L)).thenThrow(new AccessDeniedException("Forbidden"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/facilities/55"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addressEndpointShouldReturnNormalizedAddressFields() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);

        FacilityAddressDto address = new FacilityAddressDto();
        address.setCityId(34L);
        address.setCityName("Istanbul");
        address.setDistrictId(3401L);
        address.setDistrictName("Kadikoy");
        address.setNeighborhoodId(3401001L);
        address.setNeighborhoodName("Moda");
        address.setRegionId(3401001001L);
        address.setRegionName("Central");
        address.setAddressText("Street 1");
        when(facilityService.getFacilityAddressById(77L)).thenReturn(address);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/facilities/77/address"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.cityId").value(34))
                .andExpect(jsonPath("$.data.districtName").value("Kadikoy"))
                .andExpect(jsonPath("$.data.neighborhoodName").value("Moda"))
                .andExpect(jsonPath("$.data.regionName").value("Central"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }
}
