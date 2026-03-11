ALTER TABLE revision_offers
    ADD COLUMN IF NOT EXISTS labor_description TEXT;

CREATE TABLE IF NOT EXISTS revision_offer_items (
    id BIGSERIAL PRIMARY KEY,
    revision_offer_id BIGINT NOT NULL REFERENCES revision_offers(id) ON DELETE CASCADE,
    part_id BIGINT NOT NULL REFERENCES parts(id),
    quantity INTEGER NOT NULL,
    unit_price DOUBLE PRECISION NOT NULL,
    description TEXT
);

CREATE INDEX IF NOT EXISTS idx_revision_offer_items_offer_id
    ON revision_offer_items(revision_offer_id);

CREATE INDEX IF NOT EXISTS idx_revision_offer_items_part_id
    ON revision_offer_items(part_id);
