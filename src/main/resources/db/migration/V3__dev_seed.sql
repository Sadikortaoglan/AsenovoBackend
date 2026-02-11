-- ============================================================
-- V3: DEV SEED DATA (Development only - safe, no constraint violations)
-- ============================================================
-- All enum values explicitly cast
-- maintenance_sessions respects chk_session_completion_requires_qr
-- No RANDOM() for critical constraint columns
-- ============================================================

-- 1. USERS (Additional dev users - PATRON + PERSONEL)
-- Password for all users: "password"
INSERT INTO users (username, password_hash, role, active, created_at)
VALUES
    ('patron2', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PATRON', true, CURRENT_TIMESTAMP),
    ('technician1', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PERSONEL', true, CURRENT_TIMESTAMP),
    ('technician2', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PERSONEL', true, CURRENT_TIMESTAMP),
    ('technician3', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PERSONEL', true, CURRENT_TIMESTAMP),
    ('admin', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PATRON', true, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- 2. BUILDINGS (5 buildings)
INSERT INTO buildings (name, address, city, district, created_at, updated_at)
VALUES
    ('Central Business Center', '123 Main Street', 'Istanbul', 'Kadikoy', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Residential Complex Block A', '456 Oak Avenue', 'Istanbul', 'Besiktas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Residential Complex Block B', '789 Pine Road', 'Istanbul', 'Sisli', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Shopping Mall Plaza', '321 Commerce Blvd', 'Ankara', 'Cankaya', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Office Tower Alpha', '654 Business Park', 'Izmir', 'Konak', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 3. CURRENT ACCOUNTS (Auto-create for each building)
INSERT INTO current_accounts (building_id, name, authorized_person, phone, debt, credit, balance, created_at, updated_at)
SELECT 
    b.id,
    b.name || ' Account',
    'Building Manager',
    '5550000000',
    0,
    0,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM buildings b
WHERE NOT EXISTS (SELECT 1 FROM current_accounts ca WHERE ca.building_id = b.id);

-- 4. ELEVATORS (10 elevators)
INSERT INTO elevators (
    identity_number, building_name, address, elevator_number, floor_count, capacity, speed,
    technical_notes, drive_type, machine_brand, door_type, installation_year, serial_number,
    control_system, rope, modernization, inspection_date, label_date, label_type, expiry_date, status,
    building_id, blue_label, manager_name, manager_tc_identity_no, manager_phone, manager_email, created_at, updated_at
)
VALUES
    ('ELEV-001', 'Central Business Center', '123 Main Street', 'A1', 5, 630, 1.0, 'Regular maintenance', 'Hydraulic', 'Otis', 'Automatic', 2020, 'SN-001', 'Siemens', '6 ropes', '2020', CURRENT_DATE - INTERVAL '6 months', CURRENT_DATE - INTERVAL '6 months', 'BLUE'::label_type, CURRENT_DATE + INTERVAL '6 months', 'ACTIVE'::elevator_status, (SELECT id FROM buildings WHERE name = 'Central Business Center'), true, 'John Manager', '12345678901', '5551234567', 'john@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ELEV-002', 'Residential Complex Block A', '456 Oak Avenue', 'A2', 10, 1000, 1.5, 'Monthly check', 'Traction', 'Schindler', 'Automatic', 2019, 'SN-002', 'Mitsubishi', '8 ropes', NULL, CURRENT_DATE - INTERVAL '3 months', CURRENT_DATE - INTERVAL '3 months', 'GREEN'::label_type, CURRENT_DATE + INTERVAL '9 months', 'ACTIVE'::elevator_status, (SELECT id FROM buildings WHERE name = 'Residential Complex Block A'), false, 'Jane Manager', '23456789012', '5552345678', 'jane@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ELEV-003', 'Residential Complex Block B', '789 Pine Road', 'B1', 8, 800, 1.2, 'Quarterly service', 'Hydraulic', 'Kone', 'Manual', 2018, 'SN-003', 'Thyssen', '6 ropes', NULL, CURRENT_DATE - INTERVAL '12 months', CURRENT_DATE - INTERVAL '12 months', 'YELLOW'::label_type, CURRENT_DATE, 'EXPIRED'::elevator_status, (SELECT id FROM buildings WHERE name = 'Residential Complex Block B'), false, 'Bob Manager', '34567890123', '5553456789', 'bob@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ELEV-004', 'Shopping Mall Plaza', '321 Commerce Blvd', 'M1', 3, 1600, 1.0, 'High traffic', 'Hydraulic', 'Otis', 'Automatic', 2021, 'SN-004', 'Siemens', '8 ropes', NULL, CURRENT_DATE - INTERVAL '2 months', CURRENT_DATE - INTERVAL '2 months', 'GREEN'::label_type, CURRENT_DATE + INTERVAL '10 months', 'ACTIVE'::elevator_status, (SELECT id FROM buildings WHERE name = 'Shopping Mall Plaza'), false, 'Alice Manager', '45678901234', '5554567890', 'alice@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ELEV-005', 'Office Tower Alpha', '654 Business Park', 'O1', 15, 1000, 2.0, 'Fast elevator', 'Traction', 'Schindler', 'Automatic', 2022, 'SN-005', 'Mitsubishi', '10 ropes', NULL, CURRENT_DATE - INTERVAL '1 month', CURRENT_DATE - INTERVAL '1 month', 'BLUE'::label_type, CURRENT_DATE + INTERVAL '11 months', 'ACTIVE'::elevator_status, (SELECT id FROM buildings WHERE name = 'Office Tower Alpha'), true, 'Charlie Manager', '56789012345', '5555678901', 'charlie@example.com', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (identity_number) DO NOTHING;

-- 5. PARTS (10 parts)
INSERT INTO parts (name, description, unit_price, stock, created_at)
VALUES
    ('Elevator Rope 8mm', 'Standard elevator rope', 150.00, 10, CURRENT_TIMESTAMP),
    ('Motor Control Board', 'Main control board', 2500.00, 3, CURRENT_TIMESTAMP),
    ('Door Motor', 'Automatic door motor', 1800.00, 5, CURRENT_TIMESTAMP),
    ('Lighting Fixture', 'LED lighting', 85.00, 20, CURRENT_TIMESTAMP),
    ('Safety Brake', 'Emergency brake system', 3200.00, 2, CURRENT_TIMESTAMP),
    ('Cable 10mm', 'Power cable', 200.00, 8, CURRENT_TIMESTAMP),
    ('Control Panel', 'Main control panel', 1500.00, 4, CURRENT_TIMESTAMP),
    ('Speed Governor', 'Speed control device', 2800.00, 3, CURRENT_TIMESTAMP),
    ('Car Operating Panel', 'Elevator car panel', 450.00, 6, CURRENT_TIMESTAMP),
    ('Hoistway Door Lock', 'Safety door lock', 120.00, 15, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 6. MAINTENANCE TEMPLATES (2 templates)
INSERT INTO maintenance_templates (name, status, frequency_days, created_at, updated_at)
VALUES
    ('Monthly Periodic Maintenance', 'ACTIVE', 30, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('Quarterly Comprehensive Check', 'ACTIVE', 90, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

-- 7. QR PROOFS (For testing - must exist before maintenance_sessions with COMPLETED status)
INSERT INTO qr_proofs (elevator_id, token_hash, issued_at, expires_at, used_at, used_by, nonce, ip, created_at)
SELECT 
    e.id,
    'test_token_hash_' || e.id || '_' || EXTRACT(EPOCH FROM CURRENT_TIMESTAMP)::BIGINT,
    CURRENT_TIMESTAMP - INTERVAL '1 hour',
    CURRENT_TIMESTAMP + INTERVAL '1 hour',
    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    (SELECT id FROM users WHERE role = 'PERSONEL' LIMIT 1),
    'test_nonce_' || e.id,
    '127.0.0.1',
    CURRENT_TIMESTAMP
FROM elevators e
WHERE e.id IN (SELECT id FROM elevators LIMIT 3)
ON CONFLICT (token_hash) DO NOTHING;

-- 8. MAINTENANCE SESSIONS (Safe - respects constraint)
-- Rule: If status = 'COMPLETED', then started_by_qr_scan = true AND qr_proof_id IS NOT NULL
-- Rule: If status != 'COMPLETED', then qr_proof_id can be NULL

-- IN_PROGRESS sessions (qr_proof_id can be NULL)
INSERT INTO maintenance_sessions (
    plan_id, elevator_id, template_id, technician_id, start_at, end_at, status,
    started_by_qr_scan, qr_proof_id, gps_lat, gps_lng, device_info, created_at
)
SELECT 
    NULL,
    e.id,
    (SELECT id FROM maintenance_templates LIMIT 1),
    (SELECT id FROM users WHERE role = 'PERSONEL' LIMIT 1),
    CURRENT_TIMESTAMP - INTERVAL '1 hour',
    NULL,
    'IN_PROGRESS',
    false,
    NULL,
    41.0082,
    28.9784,
    'Test Device',
    CURRENT_TIMESTAMP
FROM elevators e
WHERE e.id IN (SELECT id FROM elevators LIMIT 2)
ON CONFLICT DO NOTHING;

-- COMPLETED sessions (MUST have qr_proof_id and started_by_qr_scan = true)
INSERT INTO maintenance_sessions (
    plan_id, elevator_id, template_id, technician_id, start_at, end_at, status,
    started_by_qr_scan, qr_proof_id, gps_lat, gps_lng, device_info, created_at
)
SELECT 
    NULL,
    e.id,
    (SELECT id FROM maintenance_templates LIMIT 1),
    (SELECT id FROM users WHERE role = 'PERSONEL' LIMIT 1),
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    'COMPLETED',
    true,
    qp.id,
    41.0082,
    28.9784,
    'Test Device',
    CURRENT_TIMESTAMP
FROM elevators e
INNER JOIN qr_proofs qp ON qp.elevator_id = e.id
WHERE e.id IN (SELECT id FROM elevators LIMIT 2)
ON CONFLICT DO NOTHING;
