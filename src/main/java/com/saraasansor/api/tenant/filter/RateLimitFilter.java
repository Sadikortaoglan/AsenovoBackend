package com.saraasansor.api.tenant.filter;

import com.saraasansor.api.tenant.service.PlanService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.service.EnterpriseRateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private final EnterpriseRateLimiterService rateLimiterService;
    private final PlanService planService;

    public RateLimitFilter(EnterpriseRateLimiterService rateLimiterService, PlanService planService) {
        this.rateLimiterService = rateLimiterService;
        this.planService = planService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Long tenantId = TenantContext.getTenantId();
        String endpoint = request.getRequestURI();

        // Skip rate limiting for health/error/auth and requests with no resolved tenant
        if (tenantId == null
                || !StringUtils.hasText(endpoint)
                || endpoint.startsWith("/api/health")
                || endpoint.startsWith("/api/error")
                || endpoint.startsWith("/api/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            int limit = planService.getRateLimit(tenantId);

            boolean allowed = rateLimiterService.allow(
                    tenantId,
                    endpoint,
                    limit,
                    60
            );

            if (!allowed) {
                response.setStatus(429);
                return;
            }
        } catch (Exception ex) {
            // Fail-open to avoid blocking API when tenant plan cache/backend infra is temporarily unavailable.
            log.warn("Rate limiting skipped for tenant {} and endpoint {} due to: {}", tenantId, endpoint, ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
}
