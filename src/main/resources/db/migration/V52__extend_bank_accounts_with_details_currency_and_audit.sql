ALTER TABLE bank_accounts
    ADD COLUMN IF NOT EXISTS branch_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS account_number VARCHAR(255),
    ADD COLUMN IF NOT EXISTS iban VARCHAR(64),
    ADD COLUMN IF NOT EXISTS currency VARCHAR(10) NOT NULL DEFAULT 'TRY',
    ADD COLUMN IF NOT EXISTS created_by VARCHAR(255),
    ADD COLUMN IF NOT EXISTS updated_by VARCHAR(255);

UPDATE bank_accounts
SET currency = 'TRY'
WHERE currency IS NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'ck_bank_accounts_currency'
    ) THEN
        ALTER TABLE bank_accounts
            ADD CONSTRAINT ck_bank_accounts_currency
                CHECK (currency IN ('TRY', 'USD', 'EUR', 'GBP'));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_bank_accounts_currency ON bank_accounts(currency);
CREATE INDEX IF NOT EXISTS idx_bank_accounts_iban ON bank_accounts(iban);
