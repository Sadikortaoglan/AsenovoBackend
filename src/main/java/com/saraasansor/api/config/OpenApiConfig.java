package com.saraasansor.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.method.HandlerMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class OpenApiConfig {

    private final Environment environment;

    public OpenApiConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public OpenAPI customOpenAPI() {
        OpenAPI openAPI = new OpenAPI()
                .info(new Info()
                        .title("ASENOVO Internal API")
                        .description("""
                                Internal diagnostic API documentation.

                                Kullanim:
                                1. Once tenant secin: X-Forwarded-Host header ornegi tenant1.asenovo.com
                                2. /auth/login endpointi ile giris yapin
                                3. Donen accessToken degerini Authorize alanina Bearer olarak girin
                                4. Sonraki tum istekler ayni tenant header ile gider

                                Not:
                                - Tenant bazli veriler host/header ile cozulur
                                - Revision standards gibi bazi endpointler global/shared veridir
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("ASENOVO")
                                .email("info@asenovo.com"))
                        .license(new License()
                                .name("Proprietary")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")))
                .addSecurityItem(new SecurityRequirement().addList("bearerAuth"));

        String serverUrl = environment.getProperty("asenovo.docs.server-url");
        if (serverUrl != null && !serverUrl.isBlank()) {
            List<Server> servers = new ArrayList<>();
            servers.add(new Server().url(serverUrl).description("Internal API host"));
            openAPI.setServers(servers);
        }

        return openAPI;
    }

    @Bean
    public OperationCustomizer tenantHeaderOperationCustomizer() {
        return (operation, handlerMethod) -> {
            ensureTenantHeader(operation, handlerMethod);
            return operation;
        };
    }

    private void ensureTenantHeader(io.swagger.v3.oas.models.Operation operation, HandlerMethod handlerMethod) {
        if (operation.getParameters() != null) {
            boolean alreadyDefined = operation.getParameters().stream()
                    .anyMatch(parameter -> "X-Forwarded-Host".equalsIgnoreCase(parameter.getName()));
            if (alreadyDefined) {
                return;
            }
        }

        String defaultTenantHost = environment.getProperty("asenovo.docs.default-tenant-host", "default.asenovo.local");
        List<String> tenantOptions = resolveTenantOptions();
        StringSchema schema = new StringSchema()._default(defaultTenantHost);
        if (!tenantOptions.isEmpty()) {
            schema._enum(tenantOptions);
        }
        Parameter tenantHeader = new Parameter()
                .in("header")
                .name("X-Forwarded-Host")
                .required(false)
                .description("Tenant secimi icin kullanilir. Ornek: tenant1.asenovo.com veya default.asenovo.local")
                .schema(schema);

        operation.addParametersItem(tenantHeader);
    }

    private List<String> resolveTenantOptions() {
        String configured = environment.getProperty("asenovo.docs.tenant-options", "");
        if (configured == null || configured.isBlank()) {
            return List.of();
        }

        return List.of(configured.split(",")).stream()
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }
}
