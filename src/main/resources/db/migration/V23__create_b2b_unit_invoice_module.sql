CREATE TABLE IF NOT EXISTS warehouses (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_warehouses_active_name ON warehouses(active, name);

CREATE TABLE IF NOT EXISTS b2b_unit_invoices (
    id BIGSERIAL PRIMARY KEY,
    invoice_type VARCHAR(20) NOT NULL,
    b2b_unit_id BIGINT NOT NULL REFERENCES b2b_units(id) ON DELETE RESTRICT,
    facility_id BIGINT REFERENCES facilities(id) ON DELETE RESTRICT,
    elevator_id BIGINT REFERENCES elevators(id) ON DELETE RESTRICT,
    warehouse_id BIGINT REFERENCES warehouses(id) ON DELETE RESTRICT,
    invoice_date DATE NOT NULL,
    description TEXT,
    sub_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    vat_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    grand_total NUMERIC(14,2) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_by VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_b2b_unit_invoices_type CHECK (invoice_type IN ('PURCHASE', 'SALES')),
    CONSTRAINT ck_b2b_unit_invoices_status CHECK (status IN ('DRAFT', 'POSTED')),
    CONSTRAINT ck_b2b_unit_invoices_sub_total_non_negative CHECK (sub_total >= 0),
    CONSTRAINT ck_b2b_unit_invoices_vat_total_non_negative CHECK (vat_total >= 0),
    CONSTRAINT ck_b2b_unit_invoices_grand_total_non_negative CHECK (grand_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_b2b_unit_invoices_b2b_unit_date
    ON b2b_unit_invoices(b2b_unit_id, invoice_date);

CREATE INDEX IF NOT EXISTS idx_b2b_unit_invoices_type
    ON b2b_unit_invoices(invoice_type);

CREATE TABLE IF NOT EXISTS b2b_unit_invoice_lines (
    id BIGSERIAL PRIMARY KEY,
    invoice_id BIGINT NOT NULL REFERENCES b2b_unit_invoices(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    quantity NUMERIC(14,2) NOT NULL,
    unit_price NUMERIC(14,2) NOT NULL,
    vat_rate NUMERIC(7,2) NOT NULL,
    line_sub_total NUMERIC(14,2) NOT NULL,
    line_vat_total NUMERIC(14,2) NOT NULL,
    line_grand_total NUMERIC(14,2) NOT NULL,
    sort_order INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT ck_b2b_unit_invoice_lines_quantity_positive CHECK (quantity > 0),
    CONSTRAINT ck_b2b_unit_invoice_lines_unit_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT ck_b2b_unit_invoice_lines_vat_rate_non_negative CHECK (vat_rate >= 0),
    CONSTRAINT ck_b2b_unit_invoice_lines_sub_total_non_negative CHECK (line_sub_total >= 0),
    CONSTRAINT ck_b2b_unit_invoice_lines_vat_total_non_negative CHECK (line_vat_total >= 0),
    CONSTRAINT ck_b2b_unit_invoice_lines_grand_total_non_negative CHECK (line_grand_total >= 0)
);

CREATE INDEX IF NOT EXISTS idx_b2b_unit_invoice_lines_invoice_sort
    ON b2b_unit_invoice_lines(invoice_id, sort_order);
