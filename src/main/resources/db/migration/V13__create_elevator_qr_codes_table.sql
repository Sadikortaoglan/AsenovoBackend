CREATE TABLE IF NOT EXISTS elevator_qr_codes (
    id BIGSERIAL PRIMARY KEY,
    uuid UUID NOT NULL UNIQUE,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    qr_value VARCHAR(512) NOT NULL UNIQUE,
    company_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_elevator_qr_codes_elevator_id
    ON elevator_qr_codes(elevator_id);

CREATE INDEX IF NOT EXISTS idx_elevator_qr_codes_company_id
    ON elevator_qr_codes(company_id);
