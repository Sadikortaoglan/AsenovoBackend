package com.saraasansor.api.tenant.filter;

import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.data.TenantDescriptor;
import com.saraasansor.api.tenant.data.TenantResolutionResult;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
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

    @Autowired
    public TenantResolverFilter(TenantRegistryService tenantRegistryService, Environment environment) {
        this.tenantRegistryService = tenantRegistryService;
        this.environment = environment;
    }

    public TenantResolverFilter(TenantRegistryService tenantRegistryService) {
        this(tenantRegistryService, new StandardEnvironment());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isControlPlaneEndpoint(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String host = resolveRequestHost(request);

        try {
            String subdomain = extractSubdomain(host);

            if (StringUtils.hasText(subdomain)) {
                TenantResolutionResult resolution = tenantRegistryService.resolveBySubdomain(subdomain);
                if (resolution == null) {
                    Optional<TenantDescriptor> fallback = tenantRegistryService.findActiveBySubdomain(subdomain);
                    if (fallback.isPresent()) {
                        TenantContext.setCurrentTenant(fallback.get());
                        filterChain.doFilter(request, response);
                        return;
                    }
                    writeError(response, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Unknown tenant for host: " + host);
                    return;
                }

                if (resolution.getStatus() != TenantResolutionResult.ResolutionStatus.RESOLVED
                        || resolution.getTenantDescriptor() == null) {
                    writeResolutionError(response, resolution, host);
                    return;
                }

                TenantContext.setCurrentTenant(resolution.getTenantDescriptor());
            }

            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }

    private boolean isControlPlaneEndpoint(String requestUri) {
        if (!StringUtils.hasText(requestUri)) {
            return false;
        }
        return requestUri.startsWith("/api/system-admin")
                || requestUri.startsWith("/system-admin");
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

    private void writeResolutionError(HttpServletResponse response,
                                      TenantResolutionResult resolution,
                                      String host) throws IOException {
        if (resolution == null || resolution.getStatus() == null) {
            writeError(response, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND", "Unknown tenant for host: " + host);
            return;
        }

        switch (resolution.getStatus()) {
            case TENANT_NOT_FOUND -> writeError(response, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND",
                    resolution.getMessage() != null ? resolution.getMessage() : "Unknown tenant for host: " + host);
            case SUSPENDED -> writeError(response, HttpStatus.FORBIDDEN, "TENANT_SUSPENDED",
                    resolution.getMessage() != null ? resolution.getMessage() : "Tenant is suspended");
            case EXPIRED -> writeError(response, HttpStatus.FORBIDDEN, "TENANT_EXPIRED",
                    resolution.getMessage() != null ? resolution.getMessage() : "Tenant license is expired");
            case PENDING -> writeError(response, HttpStatus.FORBIDDEN, "TENANT_PENDING",
                    resolution.getMessage() != null ? resolution.getMessage() : "Tenant provisioning is pending");
            case PROVISIONING_FAILED -> writeError(response, HttpStatus.FORBIDDEN, "TENANT_PROVISIONING_FAILED",
                    resolution.getMessage() != null ? resolution.getMessage() : "Tenant provisioning failed");
            case DELETED -> writeError(response, HttpStatus.FORBIDDEN, "TENANT_DELETED",
                    resolution.getMessage() != null ? resolution.getMessage() : "Tenant is deleted");
            case INACTIVE -> writeError(response, HttpStatus.FORBIDDEN, "TENANT_INACTIVE",
                    resolution.getMessage() != null ? resolution.getMessage() : "Tenant is inactive");
            default -> writeError(response, HttpStatus.NOT_FOUND, "TENANT_NOT_FOUND",
                    resolution.getMessage() != null ? resolution.getMessage() : "Unknown tenant for host: " + host);
        }
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status,
                            String errorCode,
                            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + errorCode + "\",\"message\":\"" + message + "\"}");
    }
}
