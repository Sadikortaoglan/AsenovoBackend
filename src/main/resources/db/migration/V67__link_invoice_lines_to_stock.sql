ALTER TABLE b2b_unit_invoice_lines
    ADD COLUMN IF NOT EXISTS stock_id BIGINT REFERENCES parts(id) ON DELETE RESTRICT;

CREATE INDEX IF NOT EXISTS idx_b2b_unit_invoice_lines_stock_id
    ON b2b_unit_invoice_lines(stock_id);
