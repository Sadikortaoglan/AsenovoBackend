package com.saraasansor.api.controller;

import com.saraasansor.api.config.GlobalExceptionHandler;
import com.saraasansor.api.dto.B2BUnitReportData;
import com.saraasansor.api.service.B2BUnitReportService;
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

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class B2BUnitReportControllerTenantIntegrationTest {

    @Test
    void reportHtmlEndpointShouldWorkWithResolvedTenantContext() throws Exception {
        B2BUnitReportService reportService = mock(B2BUnitReportService.class);
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

        when(reportService.buildReportHtml(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenReturn("<html><body>report</body></html>");

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitReportController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2b-units/5/report?startDate=2026-03-01&endDate=2026-03-31").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(content().string("<html><body>report</body></html>"));

        verify(reportService, times(1)).buildReportHtml(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31));
        assertThat(TenantContext.hasTenant()).isFalse();
    }

    @Test
    void reportDataEndpointShouldReturnJson() throws Exception {
        B2BUnitReportService reportService = mock(B2BUnitReportService.class);

        B2BUnitReportData data = new B2BUnitReportData();
        data.setB2bUnitName("Acme Cari");
        data.setReportType("B2BUnit Statement");
        when(reportService.getReportData(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31))).thenReturn(data);

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitReportController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/b2b-units/5/report-data?startDate=2026-03-01&endDate=2026-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.b2bUnitName").value("Acme Cari"));
    }

    @Test
    void reportHtmlEndpointShouldReturnHtmlBadRequestWhenServiceThrowsRuntime() throws Exception {
        B2BUnitReportService reportService = mock(B2BUnitReportService.class);
        when(reportService.buildReportHtml(5L, LocalDate.of(2026, 3, 1), LocalDate.of(2026, 3, 31)))
                .thenThrow(new RuntimeException("startDate is required"));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new B2BUnitReportController(reportService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(get("/b2b-units/5/report?startDate=2026-03-01&endDate=2026-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("startDate is required")));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }
}
