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
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@Component
public class TenantResolverFilter extends OncePerRequestFilter {

    private static final Set<String> DEFAULT_HOSTS = Set.of(
            "asenovo.com",
            "www.asenovo.com",
            "default.asenovo.com",
            "asenovo.local",
            "www.asenovo.local",
            "default.asenovo.local"
    );

    private final TenantRegistryService tenantRegistryService;
    private final Environment environment;

    public TenantResolverFilter(TenantRegistryService tenantRegistryService, Environment environment) {
        this.tenantRegistryService = tenantRegistryService;
        this.environment = environment;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String host = resolveRequestHost(request);

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

        String lowerHost = normalizeHost(host);

        // Local development: do not enforce tenancy based on host
        if (!StringUtils.hasText(lowerHost)
                || "localhost".equals(lowerHost)
                || isIpAddress(lowerHost)
                || DEFAULT_HOSTS.contains(lowerHost)) {
            return null;
        }

        String[] parts = lowerHost.split("\\.");
        if (parts.length < 3) {
            // No clear subdomain (e.g. example.com) -> keep current single-tenant behavior
            return null;
        }

        String subdomain = parts[0];
        if ("api".equals(subdomain) || "default".equals(subdomain) || "www".equals(subdomain)) {
            return null;
        }

        return subdomain;
    }

    private boolean isIpAddress(String host) {
        // Very simple heuristic; this is enough to avoid treating IP as subdomain-based host
        return host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }

    private String resolveRequestHost(HttpServletRequest request) {
        String forwardedHost = request.getHeader("X-Forwarded-Host");
        if (StringUtils.hasText(forwardedHost)) {
            return forwardedHost;
        }

        String serverName = request.getServerName();
        if (isDevProfile() && isLocalProxyHost(serverName)) {
            String inferredHost = extractHostFromUrlHeader(request.getHeader("Origin"));
            if (StringUtils.hasText(inferredHost)) {
                return inferredHost;
            }

            inferredHost = extractHostFromUrlHeader(request.getHeader("Referer"));
            if (StringUtils.hasText(inferredHost)) {
                return inferredHost;
            }
        }

        return serverName;
    }

    private String normalizeHost(String host) {
        if (!StringUtils.hasText(host)) {
            return null;
        }

        String normalized = host.split(",")[0].trim().toLowerCase(Locale.ROOT);
        int portSeparator = normalized.indexOf(':');
        if (portSeparator >= 0) {
            normalized = normalized.substring(0, portSeparator);
        }
        return normalized;
    }

    private boolean isDevProfile() {
        return environment.acceptsProfiles(Profiles.of("dev", "default"));
    }

    private boolean isLocalProxyHost(String host) {
        String normalized = normalizeHost(host);
        return "localhost".equals(normalized) || "127.0.0.1".equals(normalized);
    }

    private String extractHostFromUrlHeader(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }

        try {
            URI uri = URI.create(value.trim());
            return normalizeHost(uri.getHost());
        } catch (Exception ignored) {
            return null;
        }
    }
}
