ALTER TABLE cash_accounts
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

UPDATE cash_accounts
SET currency = 'TRY'
WHERE currency IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_cash_accounts_currency'
    ) THEN
        ALTER TABLE cash_accounts
            ADD CONSTRAINT ck_cash_accounts_currency
                CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cash_accounts_currency ON cash_accounts(currency);
