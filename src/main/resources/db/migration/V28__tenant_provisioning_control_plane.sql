-- Control-plane tenant lifecycle extensions and persistent provisioning jobs

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS company_name VARCHAR(255);

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS status VARCHAR(32);

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS plan_type VARCHAR(32);

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS license_start_date DATE;

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS license_end_date DATE;

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS max_users INTEGER;

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS max_facilities INTEGER;

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS max_elevators INTEGER;

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

UPDATE tenants t
SET company_name = t.name
WHERE t.company_name IS NULL;

UPDATE tenants t
SET status = CASE WHEN t.active THEN 'ACTIVE' ELSE 'SUSPENDED' END
WHERE t.status IS NULL;

UPDATE tenants t
SET plan_type = CASE
                    WHEN p.plan_type::TEXT = 'ENTERPRISE' THEN 'ENTERPRISE'
                    ELSE 'PROFESSIONAL'
    END
FROM plans p
WHERE t.plan_id = p.id
  AND t.plan_type IS NULL;

UPDATE tenants
SET plan_type = 'PROFESSIONAL'
WHERE plan_type IS NULL;

UPDATE tenants
SET license_start_date = CURRENT_DATE
WHERE license_start_date IS NULL;

UPDATE tenants
SET license_end_date = (CURRENT_DATE + INTERVAL '365 days')::date
WHERE license_end_date IS NULL;

ALTER TABLE tenants
    ALTER COLUMN status SET NOT NULL;

ALTER TABLE tenants
    ALTER COLUMN plan_type SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_tenants_schema_name
    ON tenants(schema_name)
    WHERE schema_name IS NOT NULL;

ALTER TABLE tenants
    DROP CONSTRAINT IF EXISTS ck_tenants_status;

ALTER TABLE tenants
    ADD CONSTRAINT ck_tenants_status
        CHECK (status IN ('PENDING', 'ACTIVE', 'SUSPENDED', 'EXPIRED', 'DELETED', 'PROVISIONING_FAILED'));

ALTER TABLE tenants
    DROP CONSTRAINT IF EXISTS ck_tenants_plan_type;

ALTER TABLE tenants
    ADD CONSTRAINT ck_tenants_plan_type
        CHECK (plan_type IN ('TRIAL', 'BASIC', 'PROFESSIONAL', 'ENTERPRISE'));

CREATE TABLE IF NOT EXISTS tenant_provisioning_jobs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    job_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    requested_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT,
    payload_json TEXT,
    created_by VARCHAR(255),
    worker_node VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tenant_provisioning_jobs_type CHECK (job_type IN ('CREATE_TENANT', 'ACTIVATE_TENANT', 'SUSPEND_TENANT', 'EXTEND_LICENSE', 'DELETE_TENANT', 'REBUILD_SCHEMA')),
    CONSTRAINT ck_tenant_provisioning_jobs_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED', 'CANCELLED'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_provisioning_jobs_status_requested_at
    ON tenant_provisioning_jobs(status, requested_at);

CREATE INDEX IF NOT EXISTS idx_tenant_provisioning_jobs_tenant_id
    ON tenant_provisioning_jobs(tenant_id);

CREATE TABLE IF NOT EXISTS tenant_provisioning_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    job_id BIGINT REFERENCES tenant_provisioning_jobs(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL,
    message TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_tenant_provisioning_audit_tenant_id
    ON tenant_provisioning_audit_logs(tenant_id);

CREATE INDEX IF NOT EXISTS idx_tenant_provisioning_audit_job_id
    ON tenant_provisioning_audit_logs(job_id);
