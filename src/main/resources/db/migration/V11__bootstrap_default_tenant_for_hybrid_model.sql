-- Bootstrap data for single-tenant -> hybrid tenancy transition.
-- Ensures control-plane records exist without breaking existing installations.

-- 0) Ensure control-plane objects exist (for environments where old V10 differs)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'plan_type') THEN
        CREATE TYPE plan_type AS ENUM ('PRO', 'ENTERPRISE');
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS plans (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    plan_type plan_type NOT NULL DEFAULT 'PRO'::plan_type,
    max_users INTEGER NOT NULL DEFAULT 0,
    max_assets INTEGER NOT NULL DEFAULT 0,
    api_rate_limit_per_minute INTEGER NOT NULL DEFAULT 0,
    max_storage_mb INTEGER NOT NULL DEFAULT 0,
    priority_support BOOLEAN NOT NULL DEFAULT false,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tenants (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    subdomain VARCHAR(255) NOT NULL UNIQUE,
    tenancy_mode VARCHAR(32) NOT NULL CHECK (tenancy_mode IN ('SHARED_SCHEMA', 'DEDICATED_DB')),
    schema_name VARCHAR(255),
    db_host VARCHAR(255),
    db_name VARCHAR(255),
    db_username VARCHAR(255),
    db_password VARCHAR(255),
    redis_namespace VARCHAR(255),
    plan_id BIGINT NOT NULL REFERENCES plans(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_tenants_subdomain ON tenants(subdomain);
CREATE INDEX IF NOT EXISTS idx_tenants_active ON tenants(active);

CREATE TABLE IF NOT EXISTS subscriptions (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES plans(id) ON DELETE SET NULL,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE SET NULL,
    starts_at DATE NOT NULL,
    ends_at DATE NOT NULL,
    auto_renew BOOLEAN NOT NULL DEFAULT true,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS features(
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(255),
    default_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS tenant_feature_overrides(
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE SET NULL,
    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE SET NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

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
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_tenants_shared_schema_required'
    ) THEN
        ALTER TABLE tenants
            ADD CONSTRAINT ck_tenants_shared_schema_required
                CHECK (
                    tenancy_mode <> 'SHARED_SCHEMA'
                        OR (schema_name IS NOT NULL AND length(trim(schema_name)) > 0)
                    );
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_tenants_dedicated_db_required'
    ) THEN
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
    END IF;
END $$;
