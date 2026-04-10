-- Allow async provisioning flow:
-- SHARED_SCHEMA tenants may not have schema_name yet while status is PENDING/PROVISIONING_FAILED.

ALTER TABLE tenants
    DROP CONSTRAINT IF EXISTS ck_tenants_shared_schema_required;

ALTER TABLE tenants
    ADD CONSTRAINT ck_tenants_shared_schema_required
        CHECK (
            tenancy_mode <> 'SHARED_SCHEMA'
            OR status IN ('PENDING', 'PROVISIONING_FAILED')
            OR (schema_name IS NOT NULL AND length(trim(schema_name)) > 0)
        );
