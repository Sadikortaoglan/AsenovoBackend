ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS logo_url VARCHAR(255);

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS primary_color VARCHAR(7);

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS secondary_color VARCHAR(7);

ALTER TABLE tenants
    ADD COLUMN IF NOT EXISTS branding_updated_at TIMESTAMP;

-- Keep schema backward-safe while enforcing color format when present.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_tenants_primary_color_hex'
    ) THEN
        ALTER TABLE tenants
            ADD CONSTRAINT ck_tenants_primary_color_hex
                CHECK (primary_color IS NULL OR primary_color ~ '^#[0-9A-Fa-f]{6}$');
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_tenants_secondary_color_hex'
    ) THEN
        ALTER TABLE tenants
            ADD CONSTRAINT ck_tenants_secondary_color_hex
                CHECK (secondary_color IS NULL OR secondary_color ~ '^#[0-9A-Fa-f]{6}$');
    END IF;
END $$;
