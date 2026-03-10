ALTER TABLE b2b_unit_transactions
    ADD COLUMN IF NOT EXISTS facility_id BIGINT REFERENCES facilities(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS amount NUMERIC(14,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS reference_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS reference_id BIGINT,
    ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'POSTED',
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE b2b_unit_transactions
SET amount = CASE
    WHEN debit_amount > 0 THEN debit_amount
    ELSE credit_amount
END
WHERE amount = 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_b2b_unit_transactions_status'
    ) THEN
        ALTER TABLE b2b_unit_transactions
            ADD CONSTRAINT ck_b2b_unit_transactions_status
            CHECK (status IN ('POSTED'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_b2b_unit_transactions_facility
    ON b2b_unit_transactions (facility_id);
