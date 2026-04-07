CREATE TABLE IF NOT EXISTS stock_units (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    abbreviation VARCHAR(100) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_stock_units_name'
    ) THEN
        ALTER TABLE stock_units
            ADD CONSTRAINT uk_stock_units_name UNIQUE (name);
    END IF;
END $$;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_stock_units_abbreviation'
    ) THEN
        ALTER TABLE stock_units
            ADD CONSTRAINT uk_stock_units_abbreviation UNIQUE (abbreviation);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_stock_units_active_name ON stock_units(active, name);
CREATE INDEX IF NOT EXISTS idx_stock_units_active_abbreviation ON stock_units(active, abbreviation);
