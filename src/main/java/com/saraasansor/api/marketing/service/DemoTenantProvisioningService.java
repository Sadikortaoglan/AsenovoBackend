package com.saraasansor.api.marketing.service;

import com.saraasansor.api.marketing.dto.TrialProvisionResponseDto;
import com.saraasansor.api.marketing.dto.TrialRequestDto;
import com.saraasansor.api.marketing.repository.TrialRequestProjection;
import com.saraasansor.api.tenant.model.Plan;
import com.saraasansor.api.tenant.model.Subscription;
import com.saraasansor.api.tenant.model.Tenant;
import com.saraasansor.api.tenant.DedicatedTenantDataSourceRegistry;
import com.saraasansor.api.tenant.repository.PlanRepository;
import com.saraasansor.api.tenant.repository.SubscriptionRepository;
import com.saraasansor.api.tenant.repository.TenantRepository;
import com.saraasansor.api.tenant.service.TenantRegistryService;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class DemoTenantProvisioningService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final char[] PASSWORD_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789".toCharArray();
    private final Object templateBuildLock = new Object();

    private final TenantRepository tenantRepository;
    private final PlanRepository planRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final TenantRegistryService tenantRegistryService;
    private final DedicatedTenantDataSourceRegistry dedicatedTenantDataSourceRegistry;
    private final DataSourceProperties sharedDataSourceProperties;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${asenovo.marketing.app-base-domain:asenovo.com}")
    private String appBaseDomain;

    @Value("${asenovo.marketing.demo-db.admin-url:}")
    private String demoDbAdminUrl;

    @Value("${asenovo.marketing.demo-db.admin-username:}")
    private String demoDbAdminUsername;

    @Value("${asenovo.marketing.demo-db.admin-password:}")
    private String demoDbAdminPassword;

    @Value("${asenovo.marketing.demo-db.name-prefix:asenovo_demo_}")
    private String demoDbNamePrefix;

    @Value("${asenovo.marketing.demo-db.template-name:asenovo_demo_template}")
    private String demoDbTemplateName;

    public DemoTenantProvisioningService(TenantRepository tenantRepository,
                                         PlanRepository planRepository,
                                         SubscriptionRepository subscriptionRepository,
                                         TenantRegistryService tenantRegistryService,
                                         DedicatedTenantDataSourceRegistry dedicatedTenantDataSourceRegistry,
                                         @Qualifier("sharedDataSourceProperties") DataSourceProperties sharedDataSourceProperties,
                                         PasswordEncoder passwordEncoder,
                                         JdbcTemplate jdbcTemplate) {
        this.tenantRepository = tenantRepository;
        this.planRepository = planRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRegistryService = tenantRegistryService;
        this.dedicatedTenantDataSourceRegistry = dedicatedTenantDataSourceRegistry;
        this.sharedDataSourceProperties = sharedDataSourceProperties;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public TrialProvisionResponseDto provisionDemoTenant(TrialRequestDto request) {
        String tenantSlug = generateTenantSlug();
        String databaseName = buildDatabaseName(tenantSlug);
        String temporaryPassword = generateTemporaryPassword();
        LocalDate expiresOn = LocalDate.now().plusDays(7);
        OffsetDateTime expiresAt = expiresOn.atTime(23, 59, 59).atOffset(ZoneOffset.UTC);
        Tenant tenant = null;

        try {
            ensureTemplateDatabaseReady();
            createDedicatedDatabaseFromTemplate(databaseName);
            tenant = createTenantRecord(request, tenantSlug, databaseName);
            createSubscription(tenant, expiresOn);

            seedClonedDemoDatabase(tenant, request, temporaryPassword);
            tenantRegistryService.evictCacheForSubdomain(tenantSlug);
            dedicatedTenantDataSourceRegistry.evict(tenant.getId());

            TrialProvisionResponseDto response = new TrialProvisionResponseDto();
            response.setTenantSlug(tenantSlug);
            response.setLoginUrl(buildLoginUrl(tenantSlug));
            response.setExpiresAt(expiresAt);
            response.setStatus("READY");
            response.setUsername("system_admin");
            response.setTemporaryPassword(temporaryPassword);
            response.setTenantDatabase(databaseName);
            return response;
        } catch (RuntimeException ex) {
            if (tenant != null) {
                dedicatedTenantDataSourceRegistry.evict(tenant.getId());
            }
            dropDedicatedDatabase(databaseName);
            throw ex;
        }
    }

    @Transactional
    public String rotateExistingDemoPassword(TrialRequestProjection request) {
        Optional<Tenant> tenantOpt = tenantRepository.findBySubdomain(request.tenantSlug());
        if (tenantOpt.isEmpty() || !tenantOpt.get().isActive()) {
            throw new IllegalStateException("Aktif demo ortami bulunamadi");
        }

        String temporaryPassword = generateTemporaryPassword();
        Tenant tenant = tenantOpt.get();

        try (Connection connection = dedicatedTenantDataSourceRegistry.resolve(tenantDescriptorOf(tenant)).getConnection()) {
            updateSystemAdminPassword(connection, temporaryPassword);
            updatePortalPassword(connection, temporaryPassword);
            dedicatedTenantDataSourceRegistry.evict(tenant.getId());
            return temporaryPassword;
        } catch (Exception ex) {
            dedicatedTenantDataSourceRegistry.evict(tenant.getId());
            throw new IllegalStateException("Mevcut demo erisim bilgileri yenilenemedi", ex);
        }
    }

    @Transactional
    public void expireDemoTenant(TrialRequestProjection request) {
        tenantRepository.findBySubdomain(request.tenantSlug()).ifPresent(tenant -> {
            if (tenant.isActive()) {
                tenant.setActive(false);
                tenantRepository.save(tenant);
            }
            subscriptionRepository.findByTenantIdAndActiveTrue(tenant.getId()).ifPresent(subscription -> {
                subscription.setActive(false);
                subscriptionRepository.save(subscription);
            });
            tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
            dedicatedTenantDataSourceRegistry.evict(tenant.getId());
        });
    }

    @Transactional
    public void cleanupExpiredDemoTenant(TrialRequestProjection request) {
        tenantRepository.findBySubdomain(request.tenantSlug()).ifPresent(tenant -> {
            if (tenant.isActive()) {
                tenant.setActive(false);
                tenantRepository.save(tenant);
            }
            subscriptionRepository.findByTenantIdAndActiveTrue(tenant.getId()).ifPresent(subscription -> {
                subscription.setActive(false);
                subscriptionRepository.save(subscription);
            });
            tenantRegistryService.evictCacheForSubdomain(tenant.getSubdomain());
            dedicatedTenantDataSourceRegistry.evict(tenant.getId());
        });

        if (request.tenantDatabase() != null && !request.tenantDatabase().isBlank()) {
            dropDedicatedDatabase(request.tenantDatabase());
        }
    }

    private Tenant createTenantRecord(TrialRequestDto request, String tenantSlug, String databaseName) {
        Plan plan = planRepository.findByCodeAndActiveTrue("PRO-DEFAULT")
                .or(() -> planRepository.findFirstByPlanTypeAndActiveTrue(Plan.PlanType.PRO))
                .orElseThrow(() -> new IllegalStateException("No active default plan found for demo provisioning"));

        Tenant tenant = new Tenant();
        tenant.setName(resolveTenantName(request, tenantSlug));
        tenant.setSubdomain(tenantSlug);
        tenant.setTenancyMode(Tenant.TenancyMode.DEDICATED_DB);
        tenant.setSchemaName("public");
        tenant.setDbHost(buildTenantJdbcUrl(databaseName));
        tenant.setDbName(databaseName);
        tenant.setDbUsername(resolveDbUsername());
        tenant.setDbPassword(resolveDbPassword());
        tenant.setRedisNamespace("tenant:" + tenantSlug);
        tenant.setPlan(plan);
        tenant.setActive(true);
        return tenantRepository.save(tenant);
    }

    private void createSubscription(Tenant tenant, LocalDate expiresOn) {
        Subscription subscription = new Subscription();
        subscription.setTenant(tenant);
        subscription.setPlan(tenant.getPlan());
        subscription.setStartsAt(LocalDate.now());
        subscription.setEndsAt(expiresOn);
        subscription.setAutoRenew(false);
        subscription.setActive(true);
        subscriptionRepository.save(subscription);
    }

    private void migrateDatabase(Tenant tenant) {
        Flyway.configure()
                .dataSource(tenant.getDbHost(), tenant.getDbUsername(), tenant.getDbPassword())
                .locations("classpath:db/migration")
                .table("flyway_schema_history")
                .baselineOnMigrate(true)
                .connectRetries(1)
                .load()
                .migrate();
    }

    private void seedClonedDemoDatabase(Tenant tenant, TrialRequestDto request, String temporaryPassword) {
        try (Connection connection = dedicatedTenantDataSourceRegistry.resolve(tenantDescriptorOf(tenant)).getConnection()) {
            updateSystemAdminPassword(connection, temporaryPassword);
            Long b2bUnitId = insertDemoB2BUnit(connection, request, temporaryPassword);
            Long facilityId = insertDemoFacility(connection, request, b2bUnitId);
            insertDemoElevator(connection, request, facilityId);
        } catch (Exception ex) {
            throw new IllegalStateException("Demo tenant seed failed", ex);
        }
    }

    private void ensureTemplateDatabaseReady() {
        if (databaseExists(resolveAdminJdbcUrl(), demoDbTemplateName)) {
            return;
        }

        synchronized (templateBuildLock) {
            if (databaseExists(resolveAdminJdbcUrl(), demoDbTemplateName)) {
                return;
            }

            createEmptyDatabase(demoDbTemplateName);
            Tenant templateTenant = buildTemplateTenantDescriptor();

            try {
                migrateDatabase(templateTenant);
                seedTemplateDatabase(templateTenant);
                markTemplateDatabase();
            } catch (RuntimeException ex) {
                dedicatedTenantDataSourceRegistry.evict(templateTenant.getId());
                dropDedicatedDatabase(demoDbTemplateName);
                throw ex;
            }
        }
    }

    private void seedTemplateDatabase(Tenant templateTenant) {
        try (Connection connection = dedicatedTenantDataSourceRegistry.resolve(tenantDescriptorOf(templateTenant)).getConnection()) {
            insertDemoMaintenanceTemplate(connection);
            copyRevisionStandards(connection);
        } catch (Exception ex) {
            throw new IllegalStateException("Demo template seed failed", ex);
        } finally {
            dedicatedTenantDataSourceRegistry.evict(templateTenant.getId());
        }
    }

    private Tenant buildTemplateTenantDescriptor() {
        Tenant tenant = new Tenant();
        tenant.setId(-1L);
        tenant.setName("Demo Template");
        tenant.setSubdomain("demo-template");
        tenant.setTenancyMode(Tenant.TenancyMode.DEDICATED_DB);
        tenant.setSchemaName("public");
        tenant.setDbHost(buildTenantJdbcUrl(demoDbTemplateName));
        tenant.setDbName(demoDbTemplateName);
        tenant.setDbUsername(resolveDbUsername());
        tenant.setDbPassword(resolveDbPassword());

        Plan plan = new Plan();
        plan.setPlanType(Plan.PlanType.PRO);
        tenant.setPlan(plan);
        return tenant;
    }

    private void updateSystemAdminPassword(Connection connection, String temporaryPassword) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE users SET password_hash = ?, active = true, enabled = true, locked = false, updated_at = CURRENT_TIMESTAMP WHERE username = 'system_admin'")) {
            statement.setString(1, passwordEncoder.encode(temporaryPassword));
            statement.executeUpdate();
        }
    }

    private void updatePortalPassword(Connection connection, String temporaryPassword) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "UPDATE b2b_units SET portal_password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE portal_username = 'system_admin'")) {
            statement.setString(1, passwordEncoder.encode(temporaryPassword));
            statement.executeUpdate();
        } catch (Exception ignored) {
            // Older demo schemas may not have this column or row; system_admin user password is enough.
        }
    }

    private Long insertDemoB2BUnit(Connection connection, TrialRequestDto request, String temporaryPassword) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO b2b_units (
                    name, phone, email, currency, risk_limit, address, description,
                    portal_username, portal_password_hash, active, created_at, updated_at
                ) VALUES (?, ?, ?, 'TRY', 0, ?, ?, ?, ?, true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """
        )) {
            statement.setString(1, defaultText(request.getCompany(), request.getName() + " Demo Cari"));
            statement.setString(2, request.getPhone());
            statement.setString(3, request.getEmail());
            statement.setString(4, defaultText(request.getCompany(), request.getName()) + " demo adresi");
            statement.setString(5, "Marketing trial demo customer");
            statement.setString(6, "system_admin");
            statement.setString(7, passwordEncoder.encode(temporaryPassword));
            return readReturningId(statement);
        }
    }

    private Long insertDemoFacility(Connection connection, TrialRequestDto request, Long b2bUnitId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO facilities (
                    name, b2b_unit_id, type, invoice_type, company_title, authorized_first_name,
                    authorized_last_name, email, phone, address_text, description, status,
                    active, created_at, updated_at
                ) VALUES (?, ?, 'TUZEL_KISI', 'TICARI_FATURA', ?, ?, ?, ?, ?, ?, ?, 'ACTIVE', true, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                RETURNING id
                """
        )) {
            String company = defaultText(request.getCompany(), request.getName() + " Demo");
            statement.setString(1, company + " Tesisi");
            statement.setLong(2, b2bUnitId);
            statement.setString(3, company);
            statement.setString(4, request.getName());
            statement.setString(5, "Admin");
            statement.setString(6, request.getEmail());
            statement.setString(7, request.getPhone());
            statement.setString(8, company + " Plaza, Istanbul");
            statement.setString(9, "Auto-created marketing demo facility");
            return readReturningId(statement);
        }
    }

    private void insertDemoElevator(Connection connection, TrialRequestDto request, Long facilityId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO elevators (
                    identity_number, building_name, facility_id, address, elevator_number, floor_count,
                    capacity, speed, technical_notes, drive_type, machine_brand, door_type,
                    installation_year, serial_number, control_system, rope, modernization,
                    inspection_date, label_date, label_type, expiry_date, status, blue_label,
                    manager_name, manager_tc_identity_no, manager_phone, manager_email,
                    created_at, updated_at
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
                          CURRENT_DATE - INTERVAL '30 days', CURRENT_DATE - INTERVAL '30 days', 'BLUE',
                          CURRENT_DATE + INTERVAL '335 days', 'ACTIVE', true, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """
        )) {
            String company = defaultText(request.getCompany(), request.getName() + " Demo");
            String identity = "DEMO-" + randomSuffix(8).toUpperCase(Locale.ROOT);
            statement.setString(1, identity);
            statement.setString(2, company + " Plaza");
            statement.setLong(3, facilityId);
            statement.setString(4, company + " Plaza, Istanbul");
            statement.setString(5, "A1");
            statement.setInt(6, 8);
            statement.setInt(7, 630);
            statement.setDouble(8, 1.0);
            statement.setString(9, "Auto-created demo elevator");
            statement.setString(10, "Traction");
            statement.setString(11, "Asenovo Demo");
            statement.setString(12, "Automatic");
            statement.setInt(13, LocalDate.now().getYear() - 1);
            statement.setString(14, "SER-" + randomSuffix(6).toUpperCase(Locale.ROOT));
            statement.setString(15, "Demo Control");
            statement.setString(16, "6 ropes");
            statement.setString(17, "Demo");
            statement.setString(18, request.getName());
            statement.setString(19, "11111111111");
            statement.setString(20, request.getPhone());
            statement.setString(21, request.getEmail());
            statement.executeUpdate();
        }
    }

    private void insertDemoMaintenanceTemplate(Connection connection) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                """
                INSERT INTO maintenance_templates (name, status, frequency_days, created_at, updated_at)
                VALUES ('Demo Aylik Bakim', 'ACTIVE', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                ON CONFLICT DO NOTHING
                """
        )) {
            statement.executeUpdate();
        } catch (Exception ignored) {
            // Demo should still work even if template schema differs in some environments.
        }
    }

    private void copyRevisionStandards(Connection connection) throws Exception {
        List<Map<String, Object>> standards = jdbcTemplate.queryForList("""
                SELECT standard_code, article_no, description, tag_color, source_file_name, source_version,
                       created_at, updated_at, price
                FROM public.revision_standards
                """);

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO revision_standards (
                    standard_code, article_no, description, tag_color, source_file_name, source_version,
                    created_at, updated_at, price
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (standard_code, article_no) DO NOTHING
                """)) {
            for (Map<String, Object> row : standards) {
                statement.setString(1, (String) row.get("standard_code"));
                statement.setString(2, (String) row.get("article_no"));
                statement.setString(3, (String) row.get("description"));
                statement.setString(4, (String) row.get("tag_color"));
                statement.setString(5, (String) row.get("source_file_name"));
                statement.setString(6, (String) row.get("source_version"));
                statement.setTimestamp(7, (java.sql.Timestamp) row.get("created_at"));
                statement.setTimestamp(8, (java.sql.Timestamp) row.get("updated_at"));
                statement.setObject(9, row.get("price"));
                statement.addBatch();
            }
            statement.executeBatch();
        }

        List<Map<String, Object>> sets = jdbcTemplate.queryForList("""
                SELECT standard_code, created_at, updated_at
                FROM public.revision_standard_sets
                """);

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO revision_standard_sets (standard_code, created_at, updated_at)
                VALUES (?, ?, ?)
                ON CONFLICT (standard_code) DO NOTHING
                """)) {
            for (Map<String, Object> row : sets) {
                statement.setString(1, (String) row.get("standard_code"));
                statement.setTimestamp(2, (java.sql.Timestamp) row.get("created_at"));
                statement.setTimestamp(3, (java.sql.Timestamp) row.get("updated_at"));
                statement.addBatch();
            }
            statement.executeBatch();
        }
    }

    private void createDedicatedDatabaseFromTemplate(String databaseName) {
        String adminUrl = resolveAdminJdbcUrl();
        String adminUser = resolveDbUsername();
        String adminPassword = resolveDbPassword();

        try (Connection connection = DriverManager.getConnection(adminUrl, adminUser, adminPassword)) {
            if (databaseExists(connection, databaseName)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE " + quoteIdentifier(databaseName) + " WITH TEMPLATE " + quoteIdentifier(demoDbTemplateName));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not clone dedicated demo database from template", ex);
        }
    }

    private void createEmptyDatabase(String databaseName) {
        String adminUrl = resolveAdminJdbcUrl();
        String adminUser = resolveDbUsername();
        String adminPassword = resolveDbPassword();

        try (Connection connection = DriverManager.getConnection(adminUrl, adminUser, adminPassword)) {
            if (databaseExists(connection, databaseName)) {
                return;
            }

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE DATABASE " + quoteIdentifier(databaseName));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Could not create dedicated demo database", ex);
        }
    }

    private void markTemplateDatabase() {
        String adminUrl = resolveAdminJdbcUrl();
        String adminUser = resolveDbUsername();
        String adminPassword = resolveDbPassword();

        try (Connection connection = DriverManager.getConnection(adminUrl, adminUser, adminPassword);
             Statement statement = connection.createStatement()) {
            statement.execute("ALTER DATABASE " + quoteIdentifier(demoDbTemplateName) + " WITH IS_TEMPLATE = true");
        } catch (Exception ignored) {
            // Clone still works without this flag in most setups; treat as optimization, not blocker.
        }
    }

    private void dropDedicatedDatabase(String databaseName) {
        String adminUrl = resolveAdminJdbcUrl();
        String adminUser = resolveDbUsername();
        String adminPassword = resolveDbPassword();

        try (Connection connection = DriverManager.getConnection(adminUrl, adminUser, adminPassword);
             PreparedStatement terminate = connection.prepareStatement(
                     "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = ? AND pid <> pg_backend_pid()");
             Statement statement = connection.createStatement()) {
            terminate.setString(1, databaseName);
            terminate.execute();
            statement.execute("DROP DATABASE IF EXISTS " + quoteIdentifier(databaseName));
        } catch (Exception ignored) {
            // Trial cleanup failure should not mask the original provisioning error.
        }
    }

    private boolean databaseExists(Connection connection, String databaseName) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_database WHERE datname = ?")) {
            statement.setString(1, databaseName);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private boolean databaseExists(String adminUrl, String databaseName) {
        try (Connection connection = DriverManager.getConnection(adminUrl, resolveDbUsername(), resolveDbPassword())) {
            return databaseExists(connection, databaseName);
        } catch (Exception ex) {
            throw new IllegalStateException("Could not inspect dedicated demo databases", ex);
        }
    }

    private String resolveAdminJdbcUrl() {
        if (demoDbAdminUrl != null && !demoDbAdminUrl.isBlank()) {
            return demoDbAdminUrl.trim();
        }

        String sharedUrl = sharedDataSourceProperties.getUrl();
        if (sharedUrl == null || sharedUrl.isBlank()) {
            throw new IllegalStateException("spring.datasource.url is required for demo database provisioning");
        }

        int queryIndex = sharedUrl.indexOf('?');
        String querySuffix = queryIndex >= 0 ? sharedUrl.substring(queryIndex) : "";
        String baseUrl = queryIndex >= 0 ? sharedUrl.substring(0, queryIndex) : sharedUrl;
        int lastSlash = baseUrl.lastIndexOf('/');
        if (lastSlash < 0) {
            throw new IllegalStateException("Could not derive admin JDBC URL from spring.datasource.url");
        }
        return baseUrl.substring(0, lastSlash + 1) + "postgres" + querySuffix;
    }

    private String buildTenantJdbcUrl(String databaseName) {
        String adminUrl = resolveAdminJdbcUrl();
        int queryIndex = adminUrl.indexOf('?');
        String querySuffix = queryIndex >= 0 ? adminUrl.substring(queryIndex) : "";
        String baseUrl = queryIndex >= 0 ? adminUrl.substring(0, queryIndex) : adminUrl;
        int lastSlash = baseUrl.lastIndexOf('/');
        return baseUrl.substring(0, lastSlash + 1) + databaseName + querySuffix;
    }

    private String resolveDbUsername() {
        if (demoDbAdminUsername != null && !demoDbAdminUsername.isBlank()) {
            return demoDbAdminUsername.trim();
        }
        return sharedDataSourceProperties.getUsername();
    }

    private String resolveDbPassword() {
        if (demoDbAdminPassword != null && !demoDbAdminPassword.isBlank()) {
            return demoDbAdminPassword;
        }
        return sharedDataSourceProperties.getPassword();
    }

    private String buildDatabaseName(String tenantSlug) {
        return demoDbNamePrefix + tenantSlug.replace('-', '_');
    }

    private com.saraasansor.api.tenant.data.TenantDescriptor tenantDescriptorOf(Tenant tenant) {
        return new com.saraasansor.api.tenant.data.TenantDescriptor(
                tenant.getId(),
                tenant.getName(),
                tenant.getSubdomain(),
                tenant.getTenancyMode(),
                tenant.getSchemaName(),
                tenant.getDbHost(),
                tenant.getDbName(),
                tenant.getDbUsername(),
                tenant.getDbPassword(),
                tenant.getRedisNamespace(),
                tenant.getPlan().getPlanType().name()
        );
    }

    private Long readReturningId(PreparedStatement statement) throws Exception {
        try (var rs = statement.executeQuery()) {
            if (!rs.next()) {
                throw new IllegalStateException("Insert did not return id");
            }
            return rs.getLong(1);
        }
    }

    private String buildLoginUrl(String tenantSlug) {
        return "https://" + tenantSlug + "." + appBaseDomain + "/login";
    }

    private String resolveTenantName(TrialRequestDto request, String tenantSlug) {
        String base = defaultText(request.getCompany(), request.getName() + " Demo");
        return base + " (" + tenantSlug + ")";
    }

    private String defaultText(String preferred, String fallback) {
        return preferred != null && !preferred.isBlank() ? preferred.trim() : fallback;
    }

    private String generateTenantSlug() {
        String slug;
        do {
            slug = "demo-" + randomSuffix(6);
        } while (tenantRepository.existsBySubdomain(slug));
        return slug;
    }

    private String generateTemporaryPassword() {
        StringBuilder builder = new StringBuilder(12);
        for (int i = 0; i < 12; i++) {
            builder.append(PASSWORD_CHARS[RANDOM.nextInt(PASSWORD_CHARS.length)]);
        }
        return builder.toString();
    }

    private String randomSuffix(int length) {
        String alphabet = "abcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(alphabet.charAt(RANDOM.nextInt(alphabet.length())));
        }
        return builder.toString();
    }

    private String quoteIdentifier(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
