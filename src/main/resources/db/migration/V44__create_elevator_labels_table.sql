CREATE TABLE IF NOT EXISTS elevator_labels (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE RESTRICT,
    qr_code_id BIGINT NULL REFERENCES elevator_qr_codes(id) ON DELETE SET NULL,
    label_name VARCHAR(255),
    label_start_date DATE,
    label_end_date DATE,
    label_issue_date DATE,
    label_date DATE,
    expiry_date DATE,
    label_type VARCHAR(50),
    serial_number VARCHAR(255),
    contract_number VARCHAR(255),
    description TEXT,
    status VARCHAR(50),
    metadata_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_elevator_labels_elevator_id ON elevator_labels (elevator_id);
CREATE INDEX IF NOT EXISTS idx_elevator_labels_status ON elevator_labels (status);
CREATE INDEX IF NOT EXISTS idx_elevator_labels_qr_code_id ON elevator_labels (qr_code_id);

INSERT INTO elevator_labels (
    id,
    elevator_id,
    qr_code_id,
    label_name,
    label_issue_date,
    label_date,
    expiry_date,
    label_type,
    status,
    created_at,
    updated_at,
    created_by,
    updated_by
)
SELECT
    q.id,
    q.elevator_id,
    q.id,
    ('Label #' || q.id),
    e.label_date,
    e.label_date,
    e.expiry_date,
    e.label_type::text,
    COALESCE(e.status::text, 'ACTIVE'),
    q.created_at,
    q.updated_at,
    'migration',
    'migration'
FROM elevator_qr_codes q
JOIN elevators e ON e.id = q.elevator_id
WHERE NOT EXISTS (
    SELECT 1 FROM elevator_labels l WHERE l.id = q.id
);

SELECT setval(
    pg_get_serial_sequence('elevator_labels', 'id'),
    COALESCE((SELECT MAX(id) FROM elevator_labels), 1),
    true
);
