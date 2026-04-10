CREATE TABLE IF NOT EXISTS b2b_unit_transactions (
    id BIGSERIAL PRIMARY KEY,
    b2b_unit_id BIGINT NOT NULL REFERENCES b2b_units(id) ON DELETE RESTRICT,
    transaction_date DATE NOT NULL,
    transaction_type VARCHAR(30) NOT NULL,
    debit_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    credit_amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    balance_after_transaction NUMERIC(14,2) NOT NULL DEFAULT 0,
    description TEXT,
    reference_code VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_b2b_unit_transactions_transaction_type CHECK (
        transaction_type IN (
            'PURCHASE',
            'SALE',
            'COLLECTION',
            'PAYMENT',
            'MANUAL_DEBIT',
            'MANUAL_CREDIT',
            'OPENING_BALANCE'
        )
    ),
    CONSTRAINT ck_b2b_unit_transactions_debit_non_negative CHECK (debit_amount >= 0),
    CONSTRAINT ck_b2b_unit_transactions_credit_non_negative CHECK (credit_amount >= 0)
);

CREATE INDEX IF NOT EXISTS idx_b2b_unit_transactions_unit_date
    ON b2b_unit_transactions (b2b_unit_id, transaction_date);

CREATE INDEX IF NOT EXISTS idx_b2b_unit_transactions_type
    ON b2b_unit_transactions (transaction_type);
