package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.FacilityDetailResponse;
import com.saraasansor.api.service.FacilityService;
import com.saraasansor.api.service.FacilityService.FacilityAttachment;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.springframework.http.HttpHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FacilityDetailTenantIntegrationTest {

    @Test
    void topLevelFacilityDetailShouldReturnDtoPayload() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);

        FacilityDetailResponse dto = new FacilityDetailResponse();
        dto.setId(101L);
        dto.setName("Facility Detail");
        dto.setB2bUnitId(10L);
        dto.setB2bUnitName("Cari A");
        dto.setAttachmentPreviewUrl("/facilities/101/attachment");

        when(facilityService.getFacilityDetail(101L)).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/facilities/101/detail"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(101))
                .andExpect(jsonPath("$.data.name").value("Facility Detail"));
    }

    @Test
    void scopedFacilityDetailShouldReturnDtoPayloadInTenantContext() throws Exception {
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

        FacilityDetailResponse dto = new FacilityDetailResponse();
        dto.setId(202L);
        dto.setName("Scoped Facility");
        dto.setB2bUnitId(5L);
        dto.setB2bUnitName("Scoped Cari");

        when(facilityService.getFacilityDetail(5L, 202L)).thenReturn(dto);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitFacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/5/facilities/202/detail").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(202))
                .andExpect(jsonPath("$.data.name").value("Scoped Facility"));

        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void attachmentEndpointShouldReturnBinaryStreamHeaders() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);

        Path attachment = Files.createTempFile("facility-attachment", ".txt");
        Files.writeString(attachment, "facility-file-content");

        when(facilityService.getFacilityAttachment(303L))
                .thenReturn(new FacilityAttachment(attachment.getFileName().toString(), "text/plain", attachment));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/facilities/303/attachment"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "text/plain"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("inline")));
    }

    @Test
    void attachmentEndpointShouldReturnNotFoundWhenMissingAttachment() throws Exception {
        FacilityService facilityService = mock(FacilityService.class);

        when(facilityService.getFacilityAttachment(404L)).thenReturn(null);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new FacilityController(facilityService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/facilities/404/attachment"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }
}
