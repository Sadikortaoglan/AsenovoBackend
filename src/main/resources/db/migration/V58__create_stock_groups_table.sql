CREATE TABLE IF NOT EXISTS stock_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_stock_groups_name_active
    ON stock_groups (LOWER(name))
    WHERE active = true;

CREATE INDEX IF NOT EXISTS idx_stock_groups_active_name ON stock_groups(active, name);
