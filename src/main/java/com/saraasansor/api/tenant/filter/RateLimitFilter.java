package com.saraasansor.api.tenant.filter;

import com.saraasansor.api.tenant.service.PlanService;
import com.saraasansor.api.tenant.TenantContext;
import com.saraasansor.api.tenant.service.EnterpriseRateLimiterService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

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

        filterChain.doFilter(request, response);
    }
}
