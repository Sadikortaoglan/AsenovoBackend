package com.saraasansor.api.tenant.filter;

import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

@Component
public class TenantResolverFilter extends OncePerRequestFilter {

    private final TenantRegistryService tenantRegistryService;

    public TenantResolverFilter(TenantRegistryService tenantRegistryService) {
        this.tenantRegistryService = tenantRegistryService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String host = request.getServerName();

        try {
            String subdomain = extractSubdomain(host);

            if (StringUtils.hasText(subdomain)) {
                Optional<TenantDescriptor> tenantOpt = tenantRegistryService.findActiveBySubdomain(subdomain);

                if (tenantOpt.isEmpty()) {
                    // Unknown tenant for the given subdomain - fail fast
                    response.setStatus(HttpStatus.NOT_FOUND.value());
                    response.setContentType("application/json");
                    response.getWriter().write("{\"error\":\"TENANT_NOT_FOUND\",\"message\":\"Unknown tenant for host: " + host + "\"}");
                    return;
                }

                TenantContext.setCurrentTenant(tenantOpt.get());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    /**
     * Very conservative subdomain extractor:
     * - "localhost" or IP addresses -> returns null (no subdomain, keep single-tenant behavior)
     * - "acme.example.com" -> "acme"
     * - "foo.bar.example.com" -> "foo" (only first label is used)
     */
    private String extractSubdomain(String host) {
        if (!StringUtils.hasText(host)) {
            return null;
        }

        String lowerHost = host.toLowerCase();

        // Local development: do not enforce tenancy based on host
        if ("localhost".equals(lowerHost) || lowerHost.startsWith("localhost:")
                || isIpAddress(lowerHost)) {
            return null;
        }

        String[] parts = lowerHost.split("\\.");
        if (parts.length < 3) {
            // No clear subdomain (e.g. example.com) -> keep current single-tenant behavior
            return null;
        }

        return parts[0];
    }

    private boolean isIpAddress(String host) {
        // Very simple heuristic; this is enough to avoid treating IP as subdomain-based host
        return host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }
}

