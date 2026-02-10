-- Ensure manager_name is NOT NULL (if column exists and is nullable)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'elevators' 
        AND column_name = 'manager_name'
        AND is_nullable = 'YES'
    ) THEN
        -- Backfill: Set default value for existing NULL records
        UPDATE elevators
        SET manager_name = 'Yönetici Adı Girilmedi'
        WHERE manager_name IS NULL;
        
        -- Make column NOT NULL
        ALTER TABLE elevators
        ALTER COLUMN manager_name SET NOT NULL;
    END IF;
END $$;
