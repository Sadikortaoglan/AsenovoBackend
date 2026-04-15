package com.saraasansor.api.controller;

import com.saraasansor.api.model.User;
import com.saraasansor.api.repository.UserRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/platform")
public class PlatformSetupController {

    private static final Set<User.Role> PLATFORM_ADMIN_ROLES = Set.of(
            User.Role.PLATFORM_ADMIN,
            User.Role.SYSTEM_ADMIN
    );

    private final UserRepository userRepository;
    private final Environment environment;

    public PlatformSetupController(UserRepository userRepository, Environment environment) {
        this.userRepository = userRepository;
        this.environment = environment;
    }

    @GetMapping("/setup-status")
    public ResponseEntity<Map<String, Object>> getSetupStatus() {
        boolean bootstrapEnabled = environment.getProperty(
                "app.platform-admin.bootstrap.enabled",
                Boolean.class,
                environment.getProperty("PLATFORM_ADMIN_BOOTSTRAP_ENABLED", Boolean.class, false)
        );
        boolean platformAdminExists = userRepository.countByRoleInAndActiveTrue(PLATFORM_ADMIN_ROLES) > 0;
        boolean setupRequired = bootstrapEnabled && !platformAdminExists;

        return ResponseEntity.ok(Map.of(
                "bootstrapEnabled", bootstrapEnabled,
                "platformAdminExists", platformAdminExists,
                "setupRequired", setupRequired
        ));
    }
}
