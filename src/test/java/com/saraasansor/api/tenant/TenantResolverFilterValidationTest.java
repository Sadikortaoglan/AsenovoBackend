package com.saraasansor.api.tenant;

import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.data.TenantResolutionResult;
import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantResolverFilterValidationTest {

    @Test
    void shouldBlockSuspendedTenantRequests() throws Exception {
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);
        when(tenantRegistryService.resolveBySubdomain("acme"))
                .thenReturn(TenantResolutionResult.blocked(
                        TenantResolutionResult.ResolutionStatus.SUSPENDED,
                        "Tenant is suspended"
                ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2bunits").with(host("acme.example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TENANT_SUSPENDED"))
                .andExpect(jsonPath("$.message").value("Tenant is suspended"));
    }

    @Test
    void shouldAllowActiveTenantRequests() throws Exception {
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);
        when(tenantRegistryService.resolveBySubdomain("acme"))
                .thenReturn(TenantResolutionResult.resolved(
                        new TenantDescriptor(
                                1L,
                                "Acme",
                                "acme",
                                Tenant.TenancyMode.SHARED_SCHEMA,
                                "tenant_acme",
                                null,
                                null,
                                null,
                                null,
                                "tenant:acme",
                                "BASIC",
                                Tenant.TenantStatus.ACTIVE,
                                null,
                                null
                        )
                ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2bunits").with(host("acme.example.com")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string("ok"));
    }

    @Test
    void shouldBlockExpiredTenantRequests() throws Exception {
        TenantRegistryService tenantRegistryService = mock(TenantRegistryService.class);
        when(tenantRegistryService.resolveBySubdomain("acme"))
                .thenReturn(TenantResolutionResult.blocked(
                        TenantResolutionResult.ResolutionStatus.EXPIRED,
                        "Tenant license is expired"
                ));

        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .addFilter(new TenantResolverFilter(tenantRegistryService))
                .build();

        mockMvc.perform(get("/b2bunits").with(host("acme.example.com")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("TENANT_EXPIRED"))
                .andExpect(jsonPath("$.message").value("Tenant license is expired"));
    }

    private RequestPostProcessor host(String host) {
        return request -> {
            request.setServerName(host);
            return request;
        };
    }

    @RestController
    static class TestController {
        @GetMapping("/b2bunits")
        public String ok() {
            return "ok";
        }
    }
}
