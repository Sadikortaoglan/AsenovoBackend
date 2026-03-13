package com.saraasansor.api.security;

import com.saraasansor.api.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.Set;

@Component
public class PlatformAdminTenantScopeFilter extends OncePerRequestFilter {

    private static final Set<String> DEFAULT_HOSTS = Set.of(
            "asenovo.com",
            "www.asenovo.com",
            "default.asenovo.com",
            "asenovo.local",
            "www.asenovo.local",
            "default.asenovo.local",
            "localhost"
    );

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isControlPlaneOrPublicEndpoint(request.getRequestURI())) {
            filterChain.doFilter(request, response);
            return;
        }

        String jwt = getJwtFromRequest(request);
        if (!StringUtils.hasText(jwt)) {
            filterChain.doFilter(request, response);
            return;
        }

        boolean isPlatformAdmin = false;
        try {
            String username = jwtTokenProvider.getUsernameFromToken(jwt);
            if (!jwtTokenProvider.validateToken(jwt, username)) {
                filterChain.doFilter(request, response);
                return;
            }

            String role = jwtTokenProvider.getRoleFromToken(jwt);
            if (StringUtils.hasText(role)) {
                User.Role normalized = User.Role.fromExternalName(role).toCanonical();
                isPlatformAdmin = normalized.isPlatformAdmin();
            }
        } catch (Exception ignored) {
            filterChain.doFilter(request, response);
            return;
        }

        if (isPlatformAdmin && !hasTenantLikeHostContext(request)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json");
            response.getWriter().write("{\"error\":\"TENANT_CONTEXT_REQUIRED\",\"message\":\"Platform admin must select tenant context for business endpoints\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isControlPlaneOrPublicEndpoint(String requestUri) {
        if (!StringUtils.hasText(requestUri)) {
            return false;
        }
        return requestUri.startsWith("/api/system-admin")
                || requestUri.startsWith("/system-admin")
                || requestUri.startsWith("/api/auth")
                || requestUri.startsWith("/auth")
                || requestUri.startsWith("/api/health")
                || requestUri.startsWith("/health")
                || requestUri.startsWith("/api/error")
                || requestUri.startsWith("/error")
                || requestUri.startsWith("/api/swagger")
                || requestUri.startsWith("/swagger")
                || requestUri.startsWith("/api-docs")
                || requestUri.startsWith("/v3/api-docs");
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean hasTenantLikeHostContext(HttpServletRequest request) {
        String host = request.getHeader("X-Forwarded-Host");
        if (!StringUtils.hasText(host)) {
            host = request.getServerName();
        }
        if (!StringUtils.hasText(host)) {
            return false;
        }

        String normalized = host.split(",")[0].trim().toLowerCase(Locale.ROOT);
        int portSeparator = normalized.indexOf(':');
        if (portSeparator >= 0) {
            normalized = normalized.substring(0, portSeparator);
        }

        if (DEFAULT_HOSTS.contains(normalized) || isIpAddress(normalized)) {
            return false;
        }

        String[] parts = normalized.split("\\.");
        if (parts.length < 3) {
            return false;
        }

        String subdomain = parts[0];
        return !"api".equals(subdomain) && !"default".equals(subdomain) && !"www".equals(subdomain);
    }

    private boolean isIpAddress(String host) {
        return host.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
    }
}
