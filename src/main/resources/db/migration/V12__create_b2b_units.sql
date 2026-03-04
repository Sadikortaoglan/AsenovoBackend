-- B2B Unit groups
CREATE TABLE b2b_unit_groups (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_b2b_unit_groups_name UNIQUE (name)
);

-- B2B Units
CREATE TABLE b2b_units (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    tax_number VARCHAR(11),
    tax_office VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    group_id BIGINT REFERENCES b2b_unit_groups(id) ON DELETE SET NULL,
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    risk_limit NUMERIC(14,2) NOT NULL DEFAULT 0,
    address TEXT,
    description TEXT,
    portal_username VARCHAR(255),
    portal_password_hash VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_b2b_units_risk_limit_non_negative CHECK (risk_limit >= 0)
);

CREATE INDEX idx_b2b_units_group_id ON b2b_units(group_id);
CREATE INDEX idx_b2b_units_name ON b2b_units(name);
CREATE UNIQUE INDEX uk_b2b_units_portal_username ON b2b_units(portal_username)
WHERE portal_username IS NOT NULL AND active = true;
