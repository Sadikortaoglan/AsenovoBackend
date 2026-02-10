-- Create revision_offer_status enum
CREATE TYPE revision_offer_status AS ENUM ('DRAFT', 'SENT', 'APPROVED', 'REJECTED', 'CONVERTED_TO_SALE');

-- Create revision_offers table
CREATE TABLE revision_offers (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    building_id BIGINT REFERENCES buildings(id) ON DELETE SET NULL,
    current_account_id BIGINT NOT NULL REFERENCES current_accounts(id) ON DELETE RESTRICT,
    parts_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    labor_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_price NUMERIC(14,2) NOT NULL DEFAULT 0,
    status revision_offer_status NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_revision_offer_elevator ON revision_offers(elevator_id);
CREATE INDEX idx_revision_offer_building ON revision_offers(building_id);
CREATE INDEX idx_revision_offer_account ON revision_offers(current_account_id);
CREATE INDEX idx_revision_offer_status ON revision_offers(status);
