ALTER TABLE elevator_qr_codes
    ADD COLUMN IF NOT EXISTS label_type label_type,
    ADD COLUMN IF NOT EXISTS start_date DATE,
    ADD COLUMN IF NOT EXISTS end_date DATE,
    ADD COLUMN IF NOT EXISTS description VARCHAR(5000),
    ADD COLUMN IF NOT EXISTS attachment_original_file_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS attachment_storage_key VARCHAR(512),
    ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS attachment_size BIGINT;

CREATE INDEX IF NOT EXISTS idx_elevator_qr_codes_start_date
    ON elevator_qr_codes(start_date);

CREATE INDEX IF NOT EXISTS idx_elevator_qr_codes_end_date
    ON elevator_qr_codes(end_date);
