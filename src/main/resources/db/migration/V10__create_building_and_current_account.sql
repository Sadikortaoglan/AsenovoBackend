-- Create buildings table
CREATE TABLE buildings (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create current_accounts table
CREATE TABLE current_accounts (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL REFERENCES buildings(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    authorized_person VARCHAR(255) NOT NULL,
    phone VARCHAR(20) NOT NULL,
    debt NUMERIC(14,2) NOT NULL DEFAULT 0,
    credit NUMERIC(14,2) NOT NULL DEFAULT 0,
    balance NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_current_account_building UNIQUE (building_id)
);

-- Create index
CREATE INDEX idx_current_account_building ON current_accounts(building_id);

-- Add building_id to elevators (optional, for future migration)
-- For now, we keep building_name for backward compatibility
ALTER TABLE elevators
ADD COLUMN IF NOT EXISTS building_id BIGINT REFERENCES buildings(id) ON DELETE SET NULL;
