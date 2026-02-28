package com.saraasansor.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;

/**
 * Production Seed Guard
 * 
 * CRITICAL: Prevents any seed/dummy data from running in production.
 * This is a hard kill switch that throws an exception if someone tries
 * to load seed data in production profile.
 * 
 * Protection Layers:
 * 1. Flyway locations: Production only loads classpath:db/migration (excludes dev/)
 * 2. app.seed.enabled: Production must be false
 * 3. This guard: Hard kill switch that blocks execution if prod + seed.enabled=true
 */
@Configuration
public class ProductionSeedGuard {
    
    private static final Logger logger = LoggerFactory.getLogger(ProductionSeedGuard.class);
    
    private final Environment environment;
    
    @Value("${app.seed.enabled:false}")
    private boolean seedEnabled;
    
    public ProductionSeedGuard(Environment environment) {
        this.environment = environment;
    }
    
    @PostConstruct
    public void guard() {
        String[] activeProfiles = environment.getActiveProfiles();
        
        // Check if production profile is active
        boolean isProduction = false;
        for (String profile : activeProfiles) {
            if ("prod".equalsIgnoreCase(profile)) {
                isProduction = true;
                break;
            }
        }
        
        // Hard kill switch: Block seed data in production
        if (isProduction && seedEnabled) {
            throw new IllegalStateException(
                "CRITICAL ERROR: Dummy/seed data execution is BLOCKED in production. " +
                "app.seed.enabled=true is not allowed when SPRING_PROFILES_ACTIVE=dev. " +
                "This prevents accidental seed data execution in production. " +
                "Check application-prod.yml: app.seed.enabled must be false."
            );
        }
        
        // Additional check: Verify Flyway locations in production
        if (isProduction) {
            String flywayLocations = environment.getProperty("spring.flyway.locations");
            if (flywayLocations != null && flywayLocations.contains("/dev/")) {
                throw new IllegalStateException(
                    "CRITICAL ERROR: Production Flyway configuration includes dev/ folder. " +
                    "spring.flyway.locations must NOT include classpath:db/migration/dev in production. " +
                    "Current value: " + flywayLocations
                );
            }
        }
        
        // Log guard status
        if (isProduction) {
            logger.info("Production Seed Guard: ACTIVE - Seed data is BLOCKED");
            logger.info("app.seed.enabled: {}, spring.flyway.locations: {}", 
                seedEnabled, environment.getProperty("spring.flyway.locations", "not set"));
        } else {
            logger.debug("Production Seed Guard: INACTIVE - Seed data allowed (dev/test)");
        }
    }
}
