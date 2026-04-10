CREATE TABLE IF NOT EXISTS elevator_contracts (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE RESTRICT,
    contract_date DATE,
    contract_html TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    attachment_original_file_name VARCHAR(255),
    attachment_storage_key VARCHAR(512),
    attachment_content_type VARCHAR(255),
    attachment_size BIGINT,
    created_by BIGINT REFERENCES users(id),
    updated_by BIGINT REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_elevator_contracts_elevator_id
    ON elevator_contracts (elevator_id);

CREATE INDEX IF NOT EXISTS idx_elevator_contracts_contract_date
    ON elevator_contracts (contract_date);

CREATE INDEX IF NOT EXISTS idx_elevator_contracts_created_at
    ON elevator_contracts (created_at);
