-- Missing modules required by UI: labels, contracts, EDM/invoice, payments, stock, reporting

CREATE TABLE elevator_labels (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    label_name VARCHAR(100) NOT NULL UNIQUE,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    description TEXT,
    file_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE elevator_contracts (
    id BIGSERIAL PRIMARY KEY,
    elevator_id BIGINT NOT NULL REFERENCES elevators(id) ON DELETE CASCADE,
    contract_date DATE NOT NULL,
    contract_html TEXT,
    file_path VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE edm_settings (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    encrypted_password TEXT NOT NULL,
    email VARCHAR(255),
    invoice_series_earchive VARCHAR(10),
    invoice_series_efatura VARCHAR(10),
    mode VARCHAR(30) NOT NULL DEFAULT 'PRODUCTION',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoice_records (
    id BIGSERIAL PRIMARY KEY,
    invoice_no VARCHAR(100),
    invoice_date DATE NOT NULL,
    direction VARCHAR(20) NOT NULL CHECK (direction IN ('INCOMING', 'OUTGOING')),
    profile VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    sender_name VARCHAR(255),
    sender_vkn_tckn VARCHAR(20),
    receiver_name VARCHAR(255),
    receiver_vkn_tckn VARCHAR(20),
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    note TEXT,
    source VARCHAR(30) NOT NULL DEFAULT 'MANUAL',
    merged_into_id BIGINT REFERENCES invoice_records(id) ON DELETE SET NULL,
    maintenance_plan_id BIGINT UNIQUE REFERENCES maintenance_plans(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE cash_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    total_in NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_out NUMERIC(14,2) NOT NULL DEFAULT 0,
    balance NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    total_in NUMERIC(14,2) NOT NULL DEFAULT 0,
    total_out NUMERIC(14,2) NOT NULL DEFAULT 0,
    balance NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    current_account_id BIGINT REFERENCES current_accounts(id) ON DELETE SET NULL,
    building_id BIGINT REFERENCES buildings(id) ON DELETE SET NULL,
    payment_type VARCHAR(20) NOT NULL CHECK (payment_type IN ('CASH', 'BANK', 'POS')),
    amount NUMERIC(14,2) NOT NULL,
    description TEXT,
    payment_date TIMESTAMP NOT NULL,
    cash_account_id BIGINT REFERENCES cash_accounts(id) ON DELETE SET NULL,
    bank_account_id BIGINT REFERENCES bank_accounts(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_items (
    id BIGSERIAL PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    stock_group VARCHAR(100),
    model_name VARCHAR(100),
    unit VARCHAR(50),
    vat_rate NUMERIC(5,2) NOT NULL DEFAULT 20,
    purchase_price NUMERIC(14,2) NOT NULL DEFAULT 0,
    sale_price NUMERIC(14,2) NOT NULL DEFAULT 0,
    stock_in NUMERIC(14,2) NOT NULL DEFAULT 0,
    stock_out NUMERIC(14,2) NOT NULL DEFAULT 0,
    current_stock NUMERIC(14,2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE stock_transfers (
    id BIGSERIAL PRIMARY KEY,
    from_stock_id BIGINT NOT NULL REFERENCES stock_items(id) ON DELETE CASCADE,
    to_stock_id BIGINT NOT NULL REFERENCES stock_items(id) ON DELETE CASCADE,
    quantity NUMERIC(14,2) NOT NULL,
    transfer_date TIMESTAMP NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE status_detection_reports (
    id BIGSERIAL PRIMARY KEY,
    report_date DATE NOT NULL,
    building_name VARCHAR(255) NOT NULL,
    elevator_name VARCHAR(255) NOT NULL,
    identity_number VARCHAR(100),
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    file_path VARCHAR(500),
    note TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_elevator_labels_elevator_id ON elevator_labels(elevator_id);
CREATE INDEX idx_elevator_contracts_elevator_id ON elevator_contracts(elevator_id);
CREATE INDEX idx_invoice_records_direction_date ON invoice_records(direction, invoice_date);
CREATE INDEX idx_invoice_records_status ON invoice_records(status);
CREATE INDEX idx_payment_transactions_date ON payment_transactions(payment_date);
CREATE INDEX idx_payment_transactions_current_account_id ON payment_transactions(current_account_id);
CREATE INDEX idx_stock_items_group_model ON stock_items(stock_group, model_name);
CREATE INDEX idx_status_detection_reports_date ON status_detection_reports(report_date);

INSERT INTO cash_accounts(name, currency) VALUES ('MERKEZ', 'TRY') ON CONFLICT DO NOTHING;
INSERT INTO bank_accounts(name, currency) VALUES ('MERKEZ BANKA', 'TRY') ON CONFLICT DO NOTHING;
