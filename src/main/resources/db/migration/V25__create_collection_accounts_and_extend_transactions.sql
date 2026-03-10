CREATE TABLE IF NOT EXISTS cash_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS bank_accounts (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_cash_accounts_active_name ON cash_accounts(active, name);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_active_name ON bank_accounts(active, name);

ALTER TABLE b2b_unit_transactions
    ADD COLUMN IF NOT EXISTS cash_account_id BIGINT REFERENCES cash_accounts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS bank_account_id BIGINT REFERENCES bank_accounts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS due_date DATE,
    ADD COLUMN IF NOT EXISTS serial_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(30);

ALTER TABLE b2b_unit_transactions
    DROP CONSTRAINT IF EXISTS ck_b2b_unit_transactions_transaction_type;

ALTER TABLE b2b_unit_transactions
    ADD CONSTRAINT ck_b2b_unit_transactions_transaction_type CHECK (
        transaction_type IN (
            'PURCHASE',
            'SALE',
            'COLLECTION',
            'PAYMENT',
            'CASH_COLLECTION',
            'PAYTR_COLLECTION',
            'CREDIT_CARD_COLLECTION',
            'BANK_COLLECTION',
            'CHECK_COLLECTION',
            'PROMISSORY_NOTE_COLLECTION',
            'MANUAL_DEBIT',
            'MANUAL_CREDIT',
            'OPENING_BALANCE'
        )
    );

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_b2b_unit_transactions_payment_provider'
    ) THEN
        ALTER TABLE b2b_unit_transactions
            ADD CONSTRAINT ck_b2b_unit_transactions_payment_provider
            CHECK (payment_provider IS NULL OR payment_provider IN ('PAYTR'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_b2b_unit_transactions_cash_account
    ON b2b_unit_transactions (cash_account_id);

CREATE INDEX IF NOT EXISTS idx_b2b_unit_transactions_bank_account
    ON b2b_unit_transactions (bank_account_id);
