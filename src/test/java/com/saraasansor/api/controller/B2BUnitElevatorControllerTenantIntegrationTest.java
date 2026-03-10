package com.saraasansor.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.B2BUnitElevatorListItemResponse;
import com.saraasansor.api.dto.ElevatorDto;
import com.saraasansor.api.service.ElevatorService;
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

import java.time.LocalDate;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitElevatorControllerTenantIntegrationTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void listEndpointShouldUseResolvedTenantContext() throws Exception {
        ElevatorService elevatorService = mock(ElevatorService.class);
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

        B2BUnitElevatorListItemResponse item = new B2BUnitElevatorListItemResponse();
        item.setId(10L);
        item.setName("Elevator 1");
        item.setFacilityId(5L);
        item.setFacilityName("Facility A");
        item.setIdentityNumber("ID-001");
        item.setBrand("BrandX");
        item.setStatus("ACTIVE");
        item.setCreatedAt(LocalDateTime.of(2026, 3, 9, 10, 0));

        when(elevatorService.getElevatorsByB2BUnit(eq(5L), eq("elev"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(item), PageRequest.of(0, 20), 1));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitElevatorController(elevatorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/5/elevators?search=elev").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].id").value(10))
                .andExpect(jsonPath("$.data.content[0].facilityName").value("Facility A"));

        verify(elevatorService, times(1)).getElevatorsByB2BUnit(eq(5L), eq("elev"), any(Pageable.class));
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void createEndpointShouldReturnValidationErrorForMissingFacilityId() throws Exception {
        ElevatorService elevatorService = mock(ElevatorService.class);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitElevatorController(elevatorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = """
                {
                  "identityNumber": "ID-001",
                  "name": "Elevator 1",
                  "labelDate": "2026-03-09",
                  "labelType": "GREEN",
                  "expiryDate": "2027-03-09",
                  "managerName": "Manager A",
                  "managerTcIdentityNo": "12345678901",
                  "managerPhone": "05551112233"
                }
                """;

        mockMvc.perform(post("/b2b-units/5/elevators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createEndpointShouldReturnForbiddenWhenServiceRejectsAccess() throws Exception {
        ElevatorService elevatorService = mock(ElevatorService.class);
        when(elevatorService.createElevatorForB2BUnit(eq(5L), any()))
                .thenThrow(new AccessDeniedException("Forbidden"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitElevatorController(elevatorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = OBJECT_MAPPER.writeValueAsString(new ElevatorCreatePayload(
                11L,
                "ID-001",
                "Elevator 1",
                "GREEN",
                "2026-03-09",
                "2027-03-09",
                "Manager A",
                "12345678901",
                "05551112233"
        ));

        mockMvc.perform(post("/b2b-units/5/elevators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createEndpointShouldReturnSuccess() throws Exception {
        ElevatorService elevatorService = mock(ElevatorService.class);
        ElevatorDto response = new ElevatorDto();
        response.setId(22L);
        response.setIdentityNumber("ID-001");
        response.setBuildingName("Facility A");
        when(elevatorService.createElevatorForB2BUnit(eq(5L), any())).thenReturn(response);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitElevatorController(elevatorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        String requestBody = """
                {
                  "facilityId": 11,
                  "identityNumber": "ID-001",
                  "name": "Elevator 1",
                  "labelDate": "2026-03-09",
                  "labelType": "GREEN",
                  "expiryDate": "2027-03-09",
                  "managerName": "Manager A",
                  "managerTcIdentityNo": "12345678901",
                  "managerPhone": "05551112233"
                }
                """;

        mockMvc.perform(post("/b2b-units/5/elevators")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(22));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    private record ElevatorCreatePayload(
            Long facilityId,
            String identityNumber,
            String name,
            String labelType,
            String labelDate,
            String expiryDate,
            String managerName,
            String managerTcIdentityNo,
            String managerPhone
    ) {
    }
}
