ALTER TABLE parts
    ADD COLUMN IF NOT EXISTS stock_entry INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS stock_exit INTEGER NOT NULL DEFAULT 0;

UPDATE parts
SET stock_entry = COALESCE(stock_entry, COALESCE(stock, 0)),
    stock_exit = COALESCE(stock_exit, 0)
WHERE stock_entry IS NULL
   OR stock_exit IS NULL;
