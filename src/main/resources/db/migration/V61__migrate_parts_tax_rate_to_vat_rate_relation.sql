ALTER TABLE parts
    ADD COLUMN IF NOT EXISTS vat_rate_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_parts_vat_rate'
    ) THEN
        ALTER TABLE parts
            ADD CONSTRAINT fk_parts_vat_rate
                FOREIGN KEY (vat_rate_id) REFERENCES vat_rates(id);
    END IF;
END $$;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = current_schema()
          AND table_name = 'parts'
          AND column_name = 'tax_rate'
    ) THEN
        UPDATE parts p
        SET vat_rate_id = v.id
        FROM vat_rates v
        WHERE p.vat_rate_id IS NULL
          AND p.tax_rate IS NOT NULL
          AND ROUND(p.tax_rate::numeric, 2) = v.rate;
    END IF;
END $$;

ALTER TABLE parts
    DROP COLUMN IF EXISTS tax_rate;

CREATE INDEX IF NOT EXISTS idx_parts_vat_rate_id ON parts(vat_rate_id);
