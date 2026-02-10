-- Maintenance Module: Complete schema creation
-- This migration creates all tables for the new maintenance system with QR anti-fraud

-- 1. Maintenance Templates
CREATE TABLE IF NOT EXISTS maintenance_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PASSIVE')),
    frequency_days INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Maintenance Sections
CREATE TABLE IF NOT EXISTS maintenance_sections (
    id BIGSERIAL PRIMARY KEY,
    template_id BIGINT NOT NULL REFERENCES maintenance_templates(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 3. Maintenance Items
CREATE TABLE IF NOT EXISTS maintenance_items (
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

-- 4. Maintenance Plans (Calendar assignments)
CREATE TABLE IF NOT EXISTS maintenance_plans (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    template_id BIGINT NOT NULL REFERENCES maintenance_templates(id) ON DELETE CASCADE,
    planned_date DATE NOT NULL,
    assigned_technician_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PLANNED' CHECK (status IN ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. QR Proofs (Anti-fraud)
CREATE TABLE IF NOT EXISTS qr_proofs (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    issued_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    used_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    nonce VARCHAR(32) NOT NULL,
    ip VARCHAR(45),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 6. Maintenance Sessions (Actual execution instances)
CREATE TABLE IF NOT EXISTS maintenance_sessions (
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
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 7. Maintenance Step Results
CREATE TABLE IF NOT EXISTS maintenance_step_results (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES maintenance_sessions(id) ON DELETE CASCADE,
    item_id BIGINT NOT NULL REFERENCES maintenance_items(id) ON DELETE CASCADE,
    result VARCHAR(50) NOT NULL CHECK (result IN ('COMPLETED', 'ISSUE_FOUND', 'NOT_APPLICABLE')),
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 8. Maintenance Attachments
CREATE TABLE IF NOT EXISTS maintenance_attachments (
    id BIGSERIAL PRIMARY KEY,
    session_id BIGINT NOT NULL REFERENCES maintenance_sessions(id) ON DELETE CASCADE,
    item_id BIGINT REFERENCES maintenance_items(id) ON DELETE SET NULL,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_maintenance_templates_status ON maintenance_templates(status);
CREATE INDEX idx_maintenance_sections_template_id ON maintenance_sections(template_id);
CREATE INDEX idx_maintenance_sections_sort_order ON maintenance_sections(template_id, sort_order);
CREATE INDEX idx_maintenance_items_section_id ON maintenance_items(section_id);
CREATE INDEX idx_maintenance_items_sort_order ON maintenance_items(section_id, sort_order);
CREATE INDEX idx_maintenance_items_is_active ON maintenance_items(is_active);

CREATE INDEX idx_maintenance_plans_elevator_id ON maintenance_plans(elevator_id);
CREATE INDEX idx_maintenance_plans_planned_date ON maintenance_plans(planned_date);
CREATE INDEX idx_maintenance_plans_status ON maintenance_plans(status);
CREATE INDEX idx_maintenance_plans_date_status ON maintenance_plans(planned_date, status);
CREATE INDEX idx_maintenance_plans_technician ON maintenance_plans(assigned_technician_id, planned_date);

CREATE INDEX idx_qr_proofs_elevator_id ON qr_proofs(elevator_id);
CREATE INDEX idx_qr_proofs_token_hash ON qr_proofs(token_hash);
CREATE INDEX idx_qr_proofs_expires_at ON qr_proofs(expires_at);
CREATE INDEX idx_qr_proofs_used_at ON qr_proofs(used_at);

CREATE INDEX idx_maintenance_sessions_plan_id ON maintenance_sessions(plan_id);
CREATE INDEX idx_maintenance_sessions_elevator_id ON maintenance_sessions(elevator_id);
CREATE INDEX idx_maintenance_sessions_technician_id ON maintenance_sessions(technician_id);
CREATE INDEX idx_maintenance_sessions_start_at ON maintenance_sessions(technician_id, start_at);
CREATE INDEX idx_maintenance_sessions_status ON maintenance_sessions(status);
CREATE INDEX idx_maintenance_sessions_qr_proof_id ON maintenance_sessions(qr_proof_id);

CREATE INDEX idx_maintenance_step_results_session_id ON maintenance_step_results(session_id);
CREATE INDEX idx_maintenance_step_results_item_id ON maintenance_step_results(item_id);
CREATE INDEX idx_maintenance_step_results_session_item ON maintenance_step_results(session_id, item_id);

CREATE INDEX idx_maintenance_attachments_session_id ON maintenance_attachments(session_id);
CREATE INDEX idx_maintenance_attachments_item_id ON maintenance_attachments(item_id);

-- Add unique constraint: one result per session-item pair
CREATE UNIQUE INDEX IF NOT EXISTS idx_maintenance_step_results_unique 
    ON maintenance_step_results(session_id, item_id);
