CREATE TABLE IF NOT EXISTS facilities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    b2b_unit_id BIGINT NOT NULL REFERENCES b2b_units(id) ON DELETE RESTRICT,
    tax_number VARCHAR(11),
    tax_office VARCHAR(255),
    type VARCHAR(20) NOT NULL DEFAULT 'TUZEL_KISI',
    invoice_type VARCHAR(20) NOT NULL DEFAULT 'TICARI_FATURA',
    company_title VARCHAR(255),
    authorized_first_name VARCHAR(255),
    authorized_last_name VARCHAR(255),
    email VARCHAR(255),
    phone VARCHAR(50),
    facility_type VARCHAR(255),
    attendant_full_name VARCHAR(255),
    manager_flat_no VARCHAR(50),
    door_password VARCHAR(255),
    floor_count INTEGER,
    city VARCHAR(255),
    district VARCHAR(255),
    neighborhood VARCHAR(255),
    region VARCHAR(255),
    address_text TEXT,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    map_lat NUMERIC(10,7),
    map_lng NUMERIC(10,7),
    map_address_query VARCHAR(500),
    attachment_url VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_facilities_type CHECK (type IN ('TUZEL_KISI', 'GERCEK_KISI')),
    CONSTRAINT ck_facilities_invoice_type CHECK (invoice_type IN ('TICARI_FATURA', 'E_ARSIV', 'E_FATURA')),
    CONSTRAINT ck_facilities_status CHECK (status IN ('ACTIVE', 'PASSIVE')),
    CONSTRAINT ck_facilities_floor_count_non_negative CHECK (floor_count IS NULL OR floor_count >= 0),
    CONSTRAINT ck_facilities_map_lat_range CHECK (map_lat IS NULL OR (map_lat >= -90 AND map_lat <= 90)),
    CONSTRAINT ck_facilities_map_lng_range CHECK (map_lng IS NULL OR (map_lng >= -180 AND map_lng <= 180))
);

CREATE INDEX IF NOT EXISTS idx_facilities_b2b_unit_name ON facilities(b2b_unit_id, name);
CREATE INDEX IF NOT EXISTS idx_facilities_status ON facilities(status);
CREATE INDEX IF NOT EXISTS idx_facilities_active ON facilities(active);
CREATE INDEX IF NOT EXISTS idx_facilities_name ON facilities(name);
