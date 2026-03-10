DO $$
DECLARE
    schema_name TEXT := current_schema();
BEGIN
    IF to_regclass(format('%I.tenants', schema_name)) IS NULL THEN
        RETURN;
    END IF;

    EXECUTE format('ALTER TABLE %I.tenants ADD COLUMN IF NOT EXISTS logo_url VARCHAR(255)', schema_name);
    EXECUTE format('ALTER TABLE %I.tenants ADD COLUMN IF NOT EXISTS primary_color VARCHAR(7)', schema_name);
    EXECUTE format('ALTER TABLE %I.tenants ADD COLUMN IF NOT EXISTS secondary_color VARCHAR(7)', schema_name);
    EXECUTE format('ALTER TABLE %I.tenants ADD COLUMN IF NOT EXISTS branding_updated_at TIMESTAMP', schema_name);

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
                 JOIN pg_class t ON c.conrelid = t.oid
                 JOIN pg_namespace n ON t.relnamespace = n.oid
        WHERE c.conname = 'ck_tenants_primary_color_hex'
          AND t.relname = 'tenants'
          AND n.nspname = schema_name
    ) THEN
        EXECUTE format(
                'ALTER TABLE %I.tenants ADD CONSTRAINT ck_tenants_primary_color_hex CHECK (primary_color IS NULL OR primary_color ~ ''^#[0-9A-Fa-f]{6}$'')',
                schema_name
                );
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
                 JOIN pg_class t ON c.conrelid = t.oid
                 JOIN pg_namespace n ON t.relnamespace = n.oid
        WHERE c.conname = 'ck_tenants_secondary_color_hex'
          AND t.relname = 'tenants'
          AND n.nspname = schema_name
    ) THEN
        EXECUTE format(
                'ALTER TABLE %I.tenants ADD CONSTRAINT ck_tenants_secondary_color_hex CHECK (secondary_color IS NULL OR secondary_color ~ ''^#[0-9A-Fa-f]{6}$'')',
                schema_name
                );
    END IF;
END $$;
