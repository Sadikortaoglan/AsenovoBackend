CREATE TABLE IF NOT EXISTS stock_transfers (
    id BIGSERIAL PRIMARY KEY,
    transfer_date DATE NOT NULL,
    stock_id BIGINT NOT NULL REFERENCES parts(id) ON DELETE RESTRICT,
    outgoing_warehouse_id BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    incoming_warehouse_id BIGINT NOT NULL REFERENCES warehouses(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_by VARCHAR(255),
    CONSTRAINT ck_stock_transfers_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_stock_transfers_warehouses_different CHECK (outgoing_warehouse_id <> incoming_warehouse_id)
);

CREATE INDEX IF NOT EXISTS idx_stock_transfers_active_date
    ON stock_transfers(active, transfer_date, id);

CREATE INDEX IF NOT EXISTS idx_stock_transfers_stock_id
    ON stock_transfers(stock_id);

CREATE INDEX IF NOT EXISTS idx_stock_transfers_outgoing_warehouse_id
    ON stock_transfers(outgoing_warehouse_id);

CREATE INDEX IF NOT EXISTS idx_stock_transfers_incoming_warehouse_id
    ON stock_transfers(incoming_warehouse_id);
