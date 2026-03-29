ALTER TABLE revision_offers
    ADD COLUMN IF NOT EXISTS converted_to_sale_id BIGINT REFERENCES b2b_unit_invoices(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_revision_offers_status
    ON revision_offers(status);

CREATE INDEX IF NOT EXISTS idx_revision_offers_converted_to_sale_id
    ON revision_offers(converted_to_sale_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_revision_offers_converted_to_sale_id
    ON revision_offers(converted_to_sale_id)
    WHERE converted_to_sale_id IS NOT NULL;
