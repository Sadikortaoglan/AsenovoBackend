package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.FacilityDto;
import com.saraasansor.api.dto.LookupDto;
import com.saraasansor.api.model.Facility;
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
import org.springframework.http.MediaType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitFacilityControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void listEndpointShouldUseResolvedTenantContext() throws Exception {
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
        dto.setId(101L);
        dto.setName("Scoped Facility");
        dto.setStatus(Facility.FacilityStatus.ACTIVE);
        when(facilityService.getFacilitiesByB2BUnit(eq(5L), eq("fac"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(dto), PageRequest.of(0, 20), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitFacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/5/facilities?search=fac").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(101));

        verify(facilityService, times(1)).getFacilitiesByB2BUnit(eq(5L), eq("fac"), any(Pageable.class));
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void createEndpointShouldReturnValidationErrorForMissingName() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitFacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = """
                {
                  "phone": "+905551112233"
                }
                """;

        mockMvc.perform(post("/b2b-units/5/facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createEndpointShouldReturnForbiddenWhenServiceRejectsAccess() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);
        when(facilityService.createFacilityForB2BUnit(eq(5L), any()))
                .thenThrow(new AccessDeniedException("Forbidden"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitFacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = OBJECT_MAPPER.writeValueAsString(new FacilityCreatePayload(
                "Scoped Facility",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "ACTIVE",
                null,
                null,
                null,
                null
        ));

        mockMvc.perform(post("/b2b-units/5/facilities")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void facilityLookupEndpointShouldUseResolvedTenantContext() throws Exception {
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

        when(facilityService.getLookupByB2BUnit(5L, "fac"))
                .thenReturn(List.of(new LookupDto(10L, "Facility A")));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitFacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/5/facilities/lookup?query=fac").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].name").value("Facility A"));

        verify(facilityService, times(1)).getLookupByB2BUnit(5L, "fac");
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record FacilityCreatePayload(
            String name,
            String taxNumber,
            String taxOffice,
            String type,
            String invoiceType,
            String companyTitle,
            String authorizedFirstName,
            String authorizedLastName,
            String email,
            String phone,
            String facilityType,
            String attendantFullName,
            String managerFlatNo,
            String doorPassword,
            Integer floorCount,
            Long cityId,
            Long districtId,
            Long neighborhoodId,
            Long regionId,
            String status,
            String mapLat,
            String mapLng,
            String mapAddressQuery,
            String attachmentUrl
    ) {
    }
}
