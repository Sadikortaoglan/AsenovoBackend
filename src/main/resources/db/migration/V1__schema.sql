-- ============================================================
-- V1: COMPLETE DATABASE SCHEMA
-- ============================================================
-- All enums created first
-- All tables with correct types matching entities
-- All constraints enforced
-- ============================================================

-- 1. ENUM TYPES (Must be created before any table uses them)
CREATE TYPE label_type AS ENUM ('GREEN', 'BLUE', 'YELLOW', 'RED', 'ORANGE');
CREATE TYPE elevator_status AS ENUM ('ACTIVE', 'EXPIRED');
CREATE TYPE revision_offer_status AS ENUM ('DRAFT', 'SENT', 'APPROVED', 'REJECTED', 'CONVERTED_TO_SALE');
CREATE TYPE inspection_color AS ENUM ('GREEN', 'YELLOW', 'RED', 'ORANGE');

-- 2. USERS
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('PATRON', 'PERSONEL')),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. BUILDINGS
CREATE TABLE buildings (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(255) NOT NULL,
    district VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. CURRENT ACCOUNTS
CREATE TABLE current_accounts (
    id BIGSERIAL PRIMARY KEY,
    building_id BIGINT NOT NULL UNIQUE REFERENCES buildings(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    authorized_person VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    debt NUMERIC(14,2) NOT NULL DEFAULT 0,
    credit NUMERIC(14,2) NOT NULL DEFAULT 0,
    balance NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. ELEVATORS
CREATE TABLE elevators (
    id BIGSERIAL PRIMARY KEY,
    identity_number VARCHAR(255) UNIQUE NOT NULL,
    building_name VARCHAR(255) NOT NULL,
    address VARCHAR(500) NOT NULL,
    elevator_number VARCHAR(50),
    floor_count INTEGER,
    capacity INTEGER,
    speed DOUBLE PRECISION,
    technical_notes TEXT,
    drive_type VARCHAR(100),
    machine_brand VARCHAR(100),
    door_type VARCHAR(100),
    installation_year INTEGER,
    serial_number VARCHAR(100),
    control_system VARCHAR(100),
    rope VARCHAR(100),
    modernization VARCHAR(100),
    inspection_date DATE NOT NULL,
    label_date DATE NOT NULL,
    label_type label_type NOT NULL DEFAULT 'BLUE'::label_type,
    expiry_date DATE NOT NULL,
    status elevator_status NOT NULL DEFAULT 'ACTIVE'::elevator_status,
    building_id BIGINT REFERENCES buildings(id) ON DELETE SET NULL,
    blue_label BOOLEAN,
    manager_name VARCHAR(255) NOT NULL,
    manager_tc_identity_no VARCHAR(11) NOT NULL,
    manager_phone VARCHAR(20) NOT NULL,
    manager_email VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. PARTS
CREATE TABLE parts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    unit_price DOUBLE PRECISION NOT NULL,
    stock INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. MAINtenances
CREATE TABLE maintenances (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    label_type label_type NOT NULL DEFAULT 'BLUE'::label_type,
    description TEXT,
    technician_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    amount DOUBLE PRECISION,
    is_paid BOOLEAN NOT NULL DEFAULT false,
    payment_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. FAULTS
CREATE TABLE faults (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    fault_subject VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255) NOT NULL,
    building_authorized_message TEXT,
    description TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'OPEN' CHECK (status IN ('OPEN', 'COMPLETED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 9. INSPECTIONS
CREATE TABLE inspections (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    result VARCHAR(50) NOT NULL CHECK (result IN ('PASSED', 'FAILED', 'PENDING')),
    description TEXT,
    inspection_color inspection_color NOT NULL DEFAULT 'GREEN'::inspection_color,
    contacted_person_name VARCHAR(255),
    report_no VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 10. OFFERS
CREATE TABLE offers (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT REFERENCES elevators(id) ON DELETE SET NULL,
    date DATE NOT NULL,
    vat_rate DOUBLE PRECISION NOT NULL DEFAULT 20.0,
    discount_amount DOUBLE PRECISION DEFAULT 0.0,
    subtotal DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    total_amount DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACCEPTED', 'REJECTED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 11. OFFER ITEMS
CREATE TABLE offer_items (
    id BIGSERIAL PRIMARY KEY,
    offer_id BIGINT NOT NULL REFERENCES offers(id) ON DELETE CASCADE,
    part_id BIGINT NOT NULL REFERENCES parts(id) ON DELETE CASCADE,
    quantity INTEGER NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    line_total DOUBLE PRECISION NOT NULL
);

-- 12. PAYMENT RECEIPTS
CREATE TABLE payment_receipts (
    id BIGSERIAL PRIMARY KEY,
    maintenance_id BIGINT NOT NULL REFERENCES maintenances(id) ON DELETE CASCADE,
    amount DOUBLE PRECISION NOT NULL,
    payer_name VARCHAR(255) NOT NULL,
    date DATE NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 13. REVISION OFFERS
CREATE TABLE revision_offers (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    building_id BIGINT REFERENCES buildings(id) ON DELETE SET NULL,
    current_account_id BIGINT NOT NULL REFERENCES current_accounts(id) ON DELETE CASCADE,
    parts_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    labor_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_price NUMERIC(14,2) NOT NULL DEFAULT 0,
    status revision_offer_status NOT NULL DEFAULT 'DRAFT'::revision_offer_status,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 14. REFRESH TOKENS
CREATE TABLE refresh_tokens (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token VARCHAR(255) UNIQUE NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at TIMESTAMP,
    is_revoked BOOLEAN NOT NULL DEFAULT false
);

-- 15. MAINTENANCE TEMPLATES
CREATE TABLE maintenance_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PASSIVE')),
    frequency_days INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 16. MAINTENANCE SECTIONS
CREATE TABLE maintenance_sections (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES maintenance_templates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 17. MAINTENANCE ITEMS
CREATE TABLE maintenance_items (
    id BIGSERIAL PRIMARY KEY,
    section_id BIGINT NOT NULL REFERENCES maintenance_sections(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    mandatory BOOLEAN NOT NULL DEFAULT false,
    allow_photo BOOLEAN NOT NULL DEFAULT false,
    allow_note BOOLEAN NOT NULL DEFAULT true,
    sort_order INTEGER NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 18. MAINTENANCE PLANS
CREATE TABLE maintenance_plans (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    template_id BIGINT NOT NULL REFERENCES maintenance_templates(id) ON DELETE CASCADE,
    planned_date DATE NOT NULL,
    assigned_technician_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 19. QR PROOFS
CREATE TABLE qr_proofs (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) UNIQUE NOT NULL,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    used_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    nonce VARCHAR(32) NOT NULL,
    ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 20. MAINTENANCE SESSIONS (with QR constraint)
CREATE TABLE maintenance_sessions (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT REFERENCES maintenance_plans(id) ON DELETE SET NULL,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    template_id BIGINT NOT NULL REFERENCES maintenance_templates(id) ON DELETE CASCADE,
    technician_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABORTED')),
    started_by_qr_scan BOOLEAN NOT NULL DEFAULT false,
    qr_proof_id BIGINT REFERENCES qr_proofs(id) ON DELETE SET NULL,
    gps_lat DOUBLE PRECISION,
    gps_lng DOUBLE PRECISION,
    device_info VARCHAR(500),
    overall_note TEXT,
    signature_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_session_completion_requires_qr CHECK (
        (status != 'COMPLETED') OR (started_by_qr_scan = true AND qr_proof_id IS NOT NULL)
    )
);

-- 21. MAINTENANCE STEP RESULTS
CREATE TABLE maintenance_step_results (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES maintenance_sessions(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES maintenance_items(id) ON DELETE CASCADE,
    result VARCHAR(50) NOT NULL CHECK (result IN ('COMPLETED', 'ISSUE_FOUND', 'NOT_APPLICABLE')),
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 22. MAINTENANCE ATTACHMENTS
CREATE TABLE maintenance_attachments (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES maintenance_sessions(id) ON DELETE CASCADE,
    item_id BIGINT REFERENCES maintenance_items(id) ON DELETE SET NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 23. QR SCAN LOGS
CREATE TABLE qr_scan_logs (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    technician_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    session_id BIGINT REFERENCES maintenance_sessions(id) ON DELETE SET NULL,
    qr_proof_id BIGINT NOT NULL REFERENCES qr_proofs(id) ON DELETE CASCADE,
    scan_timestamp TIMESTAMP NOT NULL,
    gps_lat DOUBLE PRECISION,
    gps_lng DOUBLE PRECISION,
    device_info VARCHAR(500),
    ip_address VARCHAR(45),
    is_valid BOOLEAN NOT NULL DEFAULT true,
    validation_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 24. FILE ATTACHMENTS
CREATE TABLE file_attachments (
    id BIGSERIAL PRIMARY KEY,
    entity_type VARCHAR(50) NOT NULL CHECK (entity_type IN ('ELEVATOR', 'MAINTENANCE', 'OFFER')),
    entity_id BIGINT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size BIGINT NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    url VARCHAR(500),
    uploaded_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 25. AUDIT LOGS
CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100),
    entity_id BIGINT,
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- INDEXES
CREATE INDEX idx_elevators_building_id ON elevators(building_id);
CREATE INDEX idx_elevators_status ON elevators(status);
CREATE INDEX idx_elevators_identity_number ON elevators(identity_number);
CREATE INDEX idx_maintenances_elevator_id ON maintenances(elevator_id);
CREATE INDEX idx_maintenances_date ON maintenances(date);
CREATE INDEX idx_faults_elevator_id ON faults(elevator_id);
CREATE INDEX idx_inspections_elevator_id ON inspections(elevator_id);
CREATE INDEX idx_maintenance_plans_elevator_id ON maintenance_plans(elevator_id);
CREATE INDEX idx_maintenance_plans_planned_date_status ON maintenance_plans(planned_date, status);
CREATE INDEX idx_maintenance_sessions_elevator_id ON maintenance_sessions(elevator_id);
CREATE INDEX idx_maintenance_sessions_status ON maintenance_sessions(status);
CREATE INDEX idx_qr_proofs_elevator_id ON qr_proofs(elevator_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);
