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
            'CASH_PAYMENT',
            'CREDIT_CARD_PAYMENT',
            'BANK_PAYMENT',
            'CHECK_PAYMENT',
            'PROMISSORY_NOTE_PAYMENT',
            'MANUAL_DEBIT',
            'MANUAL_CREDIT',
            'OPENING_BALANCE'
        )
    );
