-- Tenant registry (control plane) - basic version in same database

CREATE TYPE plan_type AS ENUM ('PRO', 'ENTERPRISE');

-- PRO → 1000 req/min
-- ENTERPRISE → 5000 req/min
CREATE TABLE plans (
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

CREATE TABLE tenants (
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

CREATE INDEX idx_tenants_subdomain ON tenants(subdomain);
CREATE INDEX idx_tenants_active ON tenants(active);

CREATE TABLE subscriptions (
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

-- Feature Flag Engine
CREATE TABLE features(
                                    id BIGSERIAL PRIMARY KEY,
                                    code VARCHAR(255) NOT NULL UNIQUE,
                                    description VARCHAR(255),
                                    default_enabled BOOLEAN NOT NULL DEFAULT true,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tenant_feature_overrides(
                                    id BIGSERIAL PRIMARY KEY,
                                    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE SET NULL,
                                    feature_id BIGINT NOT NULL REFERENCES features(id) ON DELETE SET NULL,
                                    enabled BOOLEAN NOT NULL DEFAULT true,
                                    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);