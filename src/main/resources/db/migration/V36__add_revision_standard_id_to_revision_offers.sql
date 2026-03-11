ALTER TABLE revision_offers
    ADD COLUMN IF NOT EXISTS revision_standard_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_revision_offers_revision_standard_id
    ON revision_offers(revision_standard_id);
