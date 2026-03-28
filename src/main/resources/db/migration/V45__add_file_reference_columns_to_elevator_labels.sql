ALTER TABLE elevator_labels
    ADD COLUMN IF NOT EXISTS attachment_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS attachment_content_type VARCHAR(255),
    ADD COLUMN IF NOT EXISTS attachment_size BIGINT,
    ADD COLUMN IF NOT EXISTS attachment_storage_key TEXT,
    ADD COLUMN IF NOT EXISTS attachment_url TEXT;
