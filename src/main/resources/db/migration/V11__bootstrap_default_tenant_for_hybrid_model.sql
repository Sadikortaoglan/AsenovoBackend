-- Bootstrap data for single-tenant -> hybrid tenancy transition.
-- Ensures control-plane records exist without breaking existing installations.

-- 1) Minimum required plans
INSERT INTO plans (code, plan_type, max_users, max_assets, api_rate_limit_per_minute, max_storage_mb, priority_support, active)
VALUES ('PRO-DEFAULT', 'PRO'::plan_type, 25, 250, 1000, 1024, false, true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO plans (code, plan_type, max_users, max_assets, api_rate_limit_per_minute, max_storage_mb, priority_support, active)
VALUES ('ENTERPRISE-DEFAULT', 'ENTERPRISE'::plan_type, 500, 50000, 5000, 10240, true, true)
ON CONFLICT (code) DO NOTHING;

-- 2) Default tenant mapped to existing public schema data
INSERT INTO tenants (name, subdomain, tenancy_mode, schema_name, redis_namespace, plan_id, active)
SELECT
    'Default Tenant',
    'default',
    'SHARED_SCHEMA',
    'public',
    'tenant:default',
    p.id,
    true
FROM plans p
WHERE p.code = 'PRO-DEFAULT'
  AND NOT EXISTS (SELECT 1 FROM tenants t WHERE t.subdomain = 'default');

-- 3) Active subscription for default tenant
INSERT INTO subscriptions (plan_id, tenant_id, starts_at, ends_at, auto_renew, active)
SELECT
    t.plan_id,
    t.id,
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '365 days',
    true,
    true
FROM tenants t
WHERE t.subdomain = 'default'
  AND NOT EXISTS (
      SELECT 1
      FROM subscriptions s
      WHERE s.tenant_id = t.id
        AND s.active = true
  );

-- 4) Guardrail checks
ALTER TABLE tenants
    ADD CONSTRAINT ck_tenants_shared_schema_required
        CHECK (
            tenancy_mode <> 'SHARED_SCHEMA'
                OR (schema_name IS NOT NULL AND length(trim(schema_name)) > 0)
            );

ALTER TABLE tenants
    ADD CONSTRAINT ck_tenants_dedicated_db_required
        CHECK (
            tenancy_mode <> 'DEDICATED_DB'
                OR (
                db_host IS NOT NULL AND length(trim(db_host)) > 0
                    AND db_name IS NOT NULL AND length(trim(db_name)) > 0
                    AND db_username IS NOT NULL AND length(trim(db_username)) > 0
                )
            );

