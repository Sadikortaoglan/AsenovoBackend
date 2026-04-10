CREATE TABLE IF NOT EXISTS models (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    brand_id BIGINT NOT NULL REFERENCES brands(id),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'uk_models_brand_name'
    ) THEN
        ALTER TABLE models
            ADD CONSTRAINT uk_models_brand_name UNIQUE (brand_id, name);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_models_active_name ON models(active, name);
CREATE INDEX IF NOT EXISTS idx_models_brand_id ON models(brand_id);
