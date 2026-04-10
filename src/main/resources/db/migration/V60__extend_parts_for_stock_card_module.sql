ALTER TABLE parts
    ADD COLUMN IF NOT EXISTS code VARCHAR(100),
    ADD COLUMN IF NOT EXISTS barcode VARCHAR(100),
    ADD COLUMN IF NOT EXISTS vat_rate_id BIGINT REFERENCES vat_rates(id),
    ADD COLUMN IF NOT EXISTS stock_group_id BIGINT REFERENCES stock_groups(id),
    ADD COLUMN IF NOT EXISTS unit_id BIGINT REFERENCES stock_units(id),
    ADD COLUMN IF NOT EXISTS brand_id BIGINT REFERENCES brands(id),
    ADD COLUMN IF NOT EXISTS model_id BIGINT REFERENCES models(id),
    ADD COLUMN IF NOT EXISTS purchase_price DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS image_path VARCHAR(500),
    ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

ALTER TABLE parts
    DROP COLUMN IF EXISTS tax_rate;

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM parts
        WHERE active = true
          AND code IS NOT NULL
          AND BTRIM(code) <> ''
        GROUP BY LOWER(code)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'V60 blocked: active duplicate part codes detected (case-insensitive). Resolve duplicates before migration.';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM parts
        WHERE active = true
          AND barcode IS NOT NULL
          AND BTRIM(barcode) <> ''
        GROUP BY LOWER(barcode)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'V60 blocked: active duplicate part barcodes detected (case-insensitive). Resolve duplicates before migration.';
    END IF;
END $$;

DROP INDEX IF EXISTS uq_parts_code_active;
DROP INDEX IF EXISTS uq_parts_barcode_active;
CREATE UNIQUE INDEX uq_parts_code_active
    ON parts (LOWER(code))
    WHERE active = true AND code IS NOT NULL AND BTRIM(code) <> '';
CREATE UNIQUE INDEX uq_parts_barcode_active
    ON parts (LOWER(barcode))
    WHERE active = true AND barcode IS NOT NULL AND BTRIM(barcode) <> '';

CREATE INDEX IF NOT EXISTS idx_parts_active_name ON parts(active, name);
CREATE INDEX IF NOT EXISTS idx_parts_vat_rate_id ON parts(vat_rate_id);
CREATE INDEX IF NOT EXISTS idx_parts_stock_group_id ON parts(stock_group_id);
CREATE INDEX IF NOT EXISTS idx_parts_unit_id ON parts(unit_id);
CREATE INDEX IF NOT EXISTS idx_parts_brand_id ON parts(brand_id);
CREATE INDEX IF NOT EXISTS idx_parts_model_id ON parts(model_id);
