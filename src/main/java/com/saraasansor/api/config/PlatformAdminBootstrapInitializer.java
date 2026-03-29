package com.saraasansor.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PlatformAdminBootstrapInitializer implements ApplicationRunner {

    static final String SQL_TABLE_EXISTS = "SELECT to_regclass(?) IS NOT NULL";
    static final String SQL_COUNT_PLATFORM_ADMINS =
            "SELECT COUNT(*) FROM users WHERE UPPER(role) IN ('SYSTEM_ADMIN','PLATFORM_ADMIN')";
    static final String SQL_COUNT_UNKNOWN_ROLES =
            "SELECT COUNT(*) FROM users WHERE role IS NULL OR UPPER(role) NOT IN " +
                    "('SYSTEM_ADMIN','PLATFORM_ADMIN','STAFF_ADMIN','TENANT_ADMIN','STAFF_USER','CARI_USER')";
    static final String SQL_COUNT_USERNAME =
            "SELECT COUNT(*) FROM users WHERE username = ?";
    static final String SQL_COUNT_DEFAULT_SEEDED_ADMIN =
            "SELECT COUNT(*) FROM users WHERE username = ? AND password_hash = ? AND UPPER(role) IN ('SYSTEM_ADMIN','PLATFORM_ADMIN')";
    static final String SQL_INSERT_PLATFORM_ADMIN_IN_USERS =
            "INSERT INTO users (username, password_hash, role, user_type, active, enabled, locked, created_at, updated_at) " +
                    "VALUES (?, ?, 'SYSTEM_ADMIN', 'SYSTEM_ADMIN', true, true, false, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";
    static final String SQL_UPDATE_DEFAULT_SEEDED_ADMIN =
            "UPDATE users SET username = ?, password_hash = ?, role = 'SYSTEM_ADMIN', user_type = 'SYSTEM_ADMIN', " +
                    "active = true, enabled = true, locked = false, updated_at = CURRENT_TIMESTAMP " +
                    "WHERE username = ? AND password_hash = ? AND UPPER(role) IN ('SYSTEM_ADMIN','PLATFORM_ADMIN')";
    static final String SQL_DELETE_PLATFORM_USER_BY_USERNAME =
            "DELETE FROM platform_users WHERE username = ?";
    static final String SQL_SYNC_PLATFORM_USERS_FROM_USERS =
            "INSERT INTO platform_users (username, password_hash, enabled, locked, role, created_at, updated_at, last_login_at) " +
                    "SELECT username, password_hash, COALESCE(enabled, true), COALESCE(locked, false), 'ROLE_PLATFORM_ADMIN', " +
                    "COALESCE(created_at, CURRENT_TIMESTAMP), CURRENT_TIMESTAMP, last_login_at " +
                    "FROM users WHERE UPPER(role) IN ('SYSTEM_ADMIN','PLATFORM_ADMIN') " +
                    "ON CONFLICT (username) DO UPDATE SET " +
                    "password_hash = EXCLUDED.password_hash, enabled = EXCLUDED.enabled, locked = EXCLUDED.locked, " +
                    "role = EXCLUDED.role, updated_at = CURRENT_TIMESTAMP, last_login_at = EXCLUDED.last_login_at";

    private static final Logger log = LoggerFactory.getLogger(PlatformAdminBootstrapInitializer.class);
    private static final String PUBLIC_USERS_TABLE = "public.users";
    private static final String PUBLIC_PLATFORM_USERS_TABLE = "public.platform_users";
    private static final String DEFAULT_SEED_USERNAME = "system_admin";
    private static final String DEFAULT_SEED_PASSWORD_HASH = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";

    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;
    private final PlatformAdminBootstrapProperties properties;

    public PlatformAdminBootstrapInitializer(JdbcTemplate jdbcTemplate,
                                            PasswordEncoder passwordEncoder,
                                            PlatformAdminBootstrapProperties properties) {
        this.jdbcTemplate = jdbcTemplate;
        this.passwordEncoder = passwordEncoder;
        this.properties = properties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!tableExists(PUBLIC_USERS_TABLE)) {
            log.info("Platform admin bootstrap skipped: {} table does not exist yet", PUBLIC_USERS_TABLE);
            return;
        }

        long unknownRoleCount = queryCount(SQL_COUNT_UNKNOWN_ROLES);
        if (unknownRoleCount > 0) {
            log.warn("Detected {} user row(s) with unknown role values during bootstrap compatibility check", unknownRoleCount);
        }

        long platformAdminCount = queryCount(SQL_COUNT_PLATFORM_ADMINS);
        if (platformAdminCount > 0) {
            maybeRotateDefaultSeededAdmin(platformAdminCount);
            synchronizePlatformUserStore();
            log.info("Platform admin bootstrap skipped: {} platform admin account(s) already exist", platformAdminCount);
            return;
        }

        if (!properties.isEnabled()) {
            throw new IllegalStateException(
                    "No platform admin exists and platform admin bootstrap is disabled. " +
                            "Enable app.platform-admin.bootstrap.enabled and provide credentials."
            );
        }

        String username = requireNonBlank(properties.getUsername(), "app.platform-admin.bootstrap.username");
        String rawPassword = requireNonBlank(properties.getPassword(), "app.platform-admin.bootstrap.password");

        if (queryCount(SQL_COUNT_USERNAME, username) > 0) {
            throw new IllegalStateException("Configured platform admin username already exists: " + username);
        }

        String passwordHash = passwordEncoder.encode(rawPassword);
        jdbcTemplate.update(SQL_INSERT_PLATFORM_ADMIN_IN_USERS, username, passwordHash);
        synchronizePlatformUserStore();

        log.info("Initial platform admin account bootstrapped for username '{}'", username);
    }

    private void maybeRotateDefaultSeededAdmin(long platformAdminCount) {
        if (!properties.isRotateDefaultSeed()) {
            return;
        }
        if (platformAdminCount != 1L) {
            return;
        }
        String targetUsername = trimToNull(properties.getUsername());
        String targetPassword = trimToNull(properties.getPassword());
        if (targetUsername == null || targetPassword == null) {
            return;
        }

        long seededCount = queryCount(SQL_COUNT_DEFAULT_SEEDED_ADMIN, DEFAULT_SEED_USERNAME, DEFAULT_SEED_PASSWORD_HASH);
        if (seededCount == 0) {
            return;
        }

        if (!DEFAULT_SEED_USERNAME.equals(targetUsername) && queryCount(SQL_COUNT_USERNAME, targetUsername) > 0) {
            log.warn(
                    "Platform admin credential rotation skipped: configured username '{}' already exists",
                    targetUsername
            );
            return;
        }

        String passwordHash = passwordEncoder.encode(targetPassword);
        int updated = jdbcTemplate.update(
                SQL_UPDATE_DEFAULT_SEEDED_ADMIN,
                targetUsername,
                passwordHash,
                DEFAULT_SEED_USERNAME,
                DEFAULT_SEED_PASSWORD_HASH
        );
        if (updated > 0 && !DEFAULT_SEED_USERNAME.equals(targetUsername) && tableExists(PUBLIC_PLATFORM_USERS_TABLE)) {
            jdbcTemplate.update(SQL_DELETE_PLATFORM_USER_BY_USERNAME, DEFAULT_SEED_USERNAME);
        }

        if (updated > 0) {
            log.info("Default seeded platform admin credentials rotated using configured bootstrap credentials");
        }
    }

    private void synchronizePlatformUserStore() {
        if (!tableExists(PUBLIC_PLATFORM_USERS_TABLE)) {
            log.warn("Platform user synchronization skipped: {} table is not available", PUBLIC_PLATFORM_USERS_TABLE);
            return;
        }
        jdbcTemplate.update(SQL_SYNC_PLATFORM_USERS_FROM_USERS);
    }

    private boolean tableExists(String qualifiedName) {
        Boolean exists = jdbcTemplate.queryForObject(SQL_TABLE_EXISTS, Boolean.class, qualifiedName);
        return Boolean.TRUE.equals(exists);
    }

    private long queryCount(String sql, Object... args) {
        Long count = jdbcTemplate.queryForObject(sql, Long.class, args);
        return count == null ? 0L : count;
    }

    private String requireNonBlank(String value, String propertyName) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            throw new IllegalStateException("Missing required property: " + propertyName);
        }
        return normalized;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
