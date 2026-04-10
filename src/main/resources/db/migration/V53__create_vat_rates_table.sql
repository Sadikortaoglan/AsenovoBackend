CREATE TABLE IF NOT EXISTS vat_rates (
    id BIGSERIAL PRIMARY KEY,
    rate NUMERIC(5,2) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT ck_vat_rates_rate_range CHECK (rate >= 0 AND rate <= 100)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_vat_rates_rate'
    ) THEN
        ALTER TABLE vat_rates
            ADD CONSTRAINT uk_vat_rates_rate UNIQUE (rate);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_vat_rates_active_rate ON vat_rates(active, rate);
