package com.saraasansor.api.security;

import com.saraasansor.api.tenant.filter.TenantResolverFilter;
import com.saraasansor.api.tenant.filter.RateLimitFilter;
import com.saraasansor.api.marketing.filter.PublicMarketingRateLimitFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.core.env.Environment;
import org.springframework.core.Ordered;
import org.springframework.core.env.Profiles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Locale;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);
    private static final String APPLICATION_JSON = "application/json";

    private final String PLATFORM_ADMIN = "PLATFORM_ADMIN";
    private final String TENANT_ADMIN = "TENANT_ADMIN";
    // Canonical aliases used across existing matcher blocks
    private final String SYSTEM_ADMIN = "PLATFORM_ADMIN";
    private final String STAFF_ADMIN = "TENANT_ADMIN";
    private final String STAFF_USER = "STAFF_USER";
    private final String CARI_USER = "CARI_USER";

    @Autowired
    private UserDetailsService userDetailsService;
    
    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired
    private Environment environment;

    @Autowired
    private TenantResolverFilter tenantResolverFilter;

    @Autowired
    private RateLimitFilter rateLimitFilter;

    @Autowired
    private PlatformAdminTenantScopeFilter platformAdminTenantScopeFilter;

    @Autowired
    private PublicMarketingRateLimitFilter publicMarketingRateLimitFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public RoleHierarchy roleHierarchy() {
        RoleHierarchyImpl hierarchy = new RoleHierarchyImpl();
        hierarchy.setHierarchy("""
            ROLE_PLATFORM_ADMIN > ROLE_SYSTEM_ADMIN
            ROLE_TENANT_ADMIN > ROLE_STAFF_ADMIN
            ROLE_SYSTEM_ADMIN > ROLE_STAFF_ADMIN
            ROLE_STAFF_ADMIN > ROLE_STAFF_USER
            ROLE_STAFF_USER > ROLE_CARI_USER
            """);
        return hierarchy;
    }

    @Bean
    public MethodSecurityExpressionHandler methodSecurityExpressionHandler(RoleHierarchy roleHierarchy) {
        DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
        handler.setRoleHierarchy(roleHierarchy);
        return handler;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        RequestMatcher swaggerRequestMatcher = this::isAllowedSwaggerRequest;
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // OPTIONS requests (CORS preflight) - permit all - MUST be first
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                // Health check endpoint - permit all
                .requestMatchers("/health").permitAll()
                .requestMatchers(HttpMethod.GET, "/platform/setup-status", "/api/platform/setup-status").permitAll()
                // Error endpoint - permit all (needed for exception handling)
                .requestMatchers("/error").permitAll()
                // Public tenant branding assets (tenant-scoped by controller checks)
                .requestMatchers(HttpMethod.GET, "/files/**").permitAll()
                // Auth endpoints - permit all (no JWT required)
                .requestMatchers("/auth/**", "/api/auth/**", "/login", "/refresh", "/register", "/logout", "/api/login", "/api/refresh", "/api/register", "/api/logout").permitAll()
                // Marketing website public form endpoints
                .requestMatchers(
                        HttpMethod.POST,
                        "/demo-request", "/trial-request", "/plan-request", "/contact",
                        "/api/demo-request", "/api/trial-request", "/api/plan-request", "/api/contact"
                ).permitAll()
                .requestMatchers(
                        HttpMethod.GET,
                        "/trial-request/*", "/api/trial-request/*"
                ).permitAll()
                .requestMatchers("/admin/revision-standards/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, "ADMIN")
                // Platform control plane endpoints
                .requestMatchers("/system-admin/**").hasRole(PLATFORM_ADMIN)
                // Tenant-scoped user management endpoints
                .requestMatchers("/tenant-admin/**").hasAnyRole(TENANT_ADMIN, PLATFORM_ADMIN)
                .requestMatchers("/admin/**").hasRole("SUPER_ADMIN")
                // Swagger/OpenAPI endpoints - internal host only
                .requestMatchers(swaggerRequestMatcher).permitAll()
                // Users endpoint - staff administration
                .requestMatchers("/users/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN)
                // Swagger/OpenAPI endpoints - permit all
                // Note: context-path is /api, so swagger paths are relative to that
                .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/swagger-ui/index.html", "/swagger-ui.html/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/api-docs/**", "/swagger-config/**").permitAll()
                .requestMatchers("/swagger-resources/**", "/webjars/**").permitAll()
                // Legacy users endpoint is intentionally closed
                .requestMatchers("/users/**").denyAll()
                // B2BUnit detail endpoint (new detail page backbone)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/detail").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit scoped facility endpoints (detail elevator operations > facilities)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/facilities").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/facilities/lookup").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/facilities").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                // B2BUnit scoped elevator endpoints (detail elevator operations > elevators)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/elevators").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/elevators").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                // B2BUnit scoped maintenance-failure endpoint (detail elevator operations > maintenance-failure)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/maintenance-failures").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit reporting endpoints (detail reporting section)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/report").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/report-data").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit transactions endpoint (detail filter section)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/transactions").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit account transactions endpoint (manual debit/credit section)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/account-transactions/manual-debit").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/account-transactions/manual-credit").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/account-transactions/*").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit collection endpoints (detail collection section)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/collections/cash").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/collections/paytr").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/collections/credit-card").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/collections/bank").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/collections/check").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/collections/promissory-note").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/collections/*").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit payment endpoints (detail payment section)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/payments/cash").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/payments/credit-card").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/payments/bank").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/payments/check").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/payments/promissory-note").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/payments/*").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit invoice endpoints (detail invoice section)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/invoices/purchase").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2b-units/*/invoices/sales").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.GET, "/b2b-units/*/invoices/*").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit endpoints
                .requestMatchers(HttpMethod.GET, "/b2bunits").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.GET, "/b2bunits/lookup").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.GET, "/b2bunits/me").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.GET, "/b2b-units/lookup").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.GET, "/b2bunits/me").hasRole(CARI_USER)
                .requestMatchers(HttpMethod.GET, "/b2bunits/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.POST, "/b2bunits/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.PUT, "/b2bunits/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.DELETE, "/b2bunits/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // Facility endpoints
                .requestMatchers(HttpMethod.GET, "/facilities/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                .requestMatchers(HttpMethod.POST, "/facilities/import-excel").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/facilities/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.PUT, "/facilities/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.DELETE, "/facilities/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN)
                // Location lookup endpoints
                .requestMatchers(HttpMethod.GET, "/locations/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // B2BUnit group endpoints
                .requestMatchers(HttpMethod.GET, "/b2bunit-groups/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.POST, "/b2bunit-groups/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN)
                .requestMatchers(HttpMethod.PUT, "/b2bunit-groups/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN)
                .requestMatchers(HttpMethod.DELETE, "/b2bunit-groups/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN)
                // Currency endpoint
                .requestMatchers(HttpMethod.GET, "/currencies/**").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER, CARI_USER)
                // Invoice lookup endpoints
                .requestMatchers(HttpMethod.GET, "/warehouses/lookup").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.GET, "/elevators/lookup").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                // Collection lookup endpoints
                .requestMatchers(HttpMethod.GET, "/cash-accounts/lookup").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                .requestMatchers(HttpMethod.GET, "/bank-accounts/lookup").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                // E-Invoice stub endpoint
                .requestMatchers(HttpMethod.GET, "/einvoice/query").hasAnyRole(SYSTEM_ADMIN, STAFF_ADMIN, STAFF_USER)
                // Everything else requires authentication
                .anyRequest().authenticated()
            )
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(APPLICATION_JSON);
                    response.getWriter().write("{\"error\":\"Unauthorized\",\"message\":\"" + authException.getMessage() + "\"}");
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(APPLICATION_JSON);
                    response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"" + accessDeniedException.getMessage() + "\"}");
                })
            )
            .authenticationProvider(authenticationProvider())
            // Keep custom filters anchored to framework filters only; using a custom filter
            // class as the anchor causes startup failures because it has no registered order.
            .addFilterBefore(publicMarketingRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(tenantResolverFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(rateLimitFilter, TenantResolverFilter.class) //Burası conflictte UsernamePasswordAuthenticationFilter'dı. Tenant yapısından dolayı TenantResolverFilter aldım ama sıkıntı çıkarsa bakılmalı
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterAfter(platformAdminTenantScopeFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
    
    /**
     * Global CORS Filter that runs BEFORE Spring Security filters.
     * This ensures CORS headers are added to ALL responses, including error responses.
     * Order 0 = Highest priority (runs first)
     */
    @Bean
    public FilterRegistrationBean<CorsFilter> corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> allowedOriginPatterns = resolveAllowedOriginPatterns();
        
        // Apply CORS configuration
        config.setAllowedOriginPatterns(allowedOriginPatterns);
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        config.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        FilterRegistrationBean<CorsFilter> bean = new FilterRegistrationBean<>(new CorsFilter(source));
        bean.setOrder(Ordered.HIGHEST_PRECEDENCE); // Order 0 - runs before Spring Security
        return bean;
    }
    
    /**
     * CORS Configuration Source for Spring Security.
     * Used by SecurityFilterChain's .cors() configuration.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        List<String> allowedOriginPatterns = resolveAllowedOriginPatterns();
        
        // Use allowedOriginPatterns (supports wildcards with credentials)
        // Note: setAllowedOrigins("*") is incompatible with allowCredentials(true)
        configuration.setAllowedOriginPatterns(allowedOriginPatterns);
        
        // Allow all HTTP methods including OPTIONS for preflight
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Allow specific headers (Authorization, Content-Type, Accept, Origin, etc.)
        configuration.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "Accept", "Origin", 
                "X-Requested-With", "Access-Control-Request-Method", "Access-Control-Request-Headers"));
        
        // Allow credentials (required for Authorization header)
        configuration.setAllowCredentials(true);
        
        // Cache preflight response for 1 hour
        configuration.setMaxAge(3600L);
        
        // Expose headers to frontend
        configuration.setExposedHeaders(Arrays.asList("Authorization", "Content-Type"));
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    private List<String> resolveAllowedOriginPatterns() {
        List<String> allowedOriginPatterns = new ArrayList<>();

        // Local development defaults
        boolean isDevProfile = environment.acceptsProfiles(Profiles.of("dev", "default"));
        if (isDevProfile) {
            allowedOriginPatterns.add("https://*.asenovo.local");
            allowedOriginPatterns.add("https://localhost:*");
            allowedOriginPatterns.add("https://127.0.0.1:*");
            allowedOriginPatterns.add("http://localhost:*");
            allowedOriginPatterns.add("http://127.0.0.1:*");
            allowedOriginPatterns.add("http://*.asenovo.local:*");
            allowedOriginPatterns.add("http://*.lvh.me:*");
            allowedOriginPatterns.add("https://*.lvh.me:*");
        }

        // Comma-separated explicit list/patterns from env
        String corsOrigins = environment.getProperty("CORS_ALLOWED_ORIGINS");
        if (corsOrigins != null && !corsOrigins.trim().isEmpty()) {
            String[] origins = corsOrigins.split(",");
            for (String origin : origins) {
                String trimmed = origin.trim();
                if (!trimmed.isEmpty() && !allowedOriginPatterns.contains(trimmed)) {
                    allowedOriginPatterns.add(trimmed);
                }
            }
        }

        if (allowedOriginPatterns.isEmpty()) {
            log.warn("No CORS allowed origins configured. Browser requests will fail.");
        }

        return allowedOriginPatterns;
    }

    private boolean isAllowedSwaggerRequest(jakarta.servlet.http.HttpServletRequest request) {
        if (!isDocsEnabled()) {
            return false;
        }
        if (!isSwaggerPath(request.getRequestURI())) {
            return false;
        }
        return isAllowedDocsHost(request);
    }

    private boolean isDocsEnabled() {
        return environment.getProperty("asenovo.docs.enabled", Boolean.class, Boolean.TRUE);
    }

    private boolean isSwaggerPath(String requestUri) {
        return requestUri.startsWith("/api/swagger-ui")
                || requestUri.startsWith("/api/v3/api-docs")
                || requestUri.startsWith("/api/api-docs")
                || requestUri.startsWith("/api/swagger-config")
                || requestUri.startsWith("/api/swagger-resources")
                || requestUri.startsWith("/api/webjars");
    }

    private boolean isAllowedDocsHost(jakarta.servlet.http.HttpServletRequest request) {
        String requestHost = extractHost(request.getHeader("X-Forwarded-Host"));
        if (requestHost == null) {
            requestHost = extractHost(request.getHeader("Host"));
        }
        if (requestHost == null) {
            requestHost = extractHost(request.getServerName());
        }

        List<String> allowedHosts = resolveAllowedDocsHosts();
        return requestHost != null && allowedHosts.contains(requestHost.toLowerCase(Locale.ROOT));
    }

    private List<String> resolveAllowedDocsHosts() {
        List<String> hosts = new ArrayList<>();

        String configured = environment.getProperty("asenovo.docs.allowed-hosts");
        if (configured != null && !configured.isBlank()) {
            for (String host : configured.split(",")) {
                String trimmed = extractHost(host);
                if (trimmed != null && !trimmed.isBlank()) {
                    hosts.add(trimmed.toLowerCase(Locale.ROOT));
                }
            }
        }

        if (environment.acceptsProfiles(Profiles.of("dev", "default"))) {
            addHostIfMissing(hosts, "localhost");
            addHostIfMissing(hosts, "127.0.0.1");
            addHostIfMissing(hosts, "default.asenovo.local");
            addHostIfMissing(hosts, "internal-api.asenovo.local");
        }

        return hosts;
    }

    private void addHostIfMissing(List<String> hosts, String host) {
        if (!hosts.contains(host)) {
            hosts.add(host);
        }
    }

    private String extractHost(String rawHost) {
        if (rawHost == null || rawHost.isBlank()) {
            return null;
        }
        String first = rawHost.split(",")[0].trim();
        int colonIndex = first.indexOf(':');
        return colonIndex >= 0 ? first.substring(0, colonIndex) : first;
    }
}
