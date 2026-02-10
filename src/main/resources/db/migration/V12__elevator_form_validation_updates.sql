-- Add ORANGE to label_type enum if it doesn't exist
-- PostgreSQL doesn't support ALTER TYPE ADD VALUE in transaction, so we use a workaround
DO $$ 
BEGIN
    -- Check if ORANGE already exists
    IF NOT EXISTS (
        SELECT 1 FROM pg_enum 
        WHERE enumlabel = 'ORANGE' 
        AND enumtypid = (SELECT oid FROM pg_type WHERE typname = 'label_type')
    ) THEN
        -- Add ORANGE to label_type enum
        ALTER TYPE label_type ADD VALUE 'ORANGE';
    END IF;
END $$;

-- Ensure manager_tc_identity_no is NOT NULL (if column exists and is nullable)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'elevators' 
        AND column_name = 'manager_tc_identity_no'
        AND is_nullable = 'YES'
    ) THEN
        -- Backfill: Set default value for existing NULL records
        UPDATE elevators
        SET manager_tc_identity_no = '00000000000'
        WHERE manager_tc_identity_no IS NULL;
        
        -- Make column NOT NULL
        ALTER TABLE elevators
        ALTER COLUMN manager_tc_identity_no SET NOT NULL,
        ALTER COLUMN manager_tc_identity_no SET DEFAULT '00000000000';
    END IF;
END $$;

-- Ensure manager_phone is NOT NULL (if column exists and is nullable)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'elevators' 
        AND column_name = 'manager_phone'
        AND is_nullable = 'YES'
    ) THEN
        -- Backfill: Set default value for existing NULL records
        UPDATE elevators
        SET manager_phone = '0000000000'
        WHERE manager_phone IS NULL;
        
        -- Make column NOT NULL
        ALTER TABLE elevators
        ALTER COLUMN manager_phone SET NOT NULL,
        ALTER COLUMN manager_phone SET DEFAULT '0000000000';
    END IF;
END $$;

-- Ensure label_type is NOT NULL (should already be, but double-check)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'elevators' 
        AND column_name = 'label_type'
        AND is_nullable = 'YES'
    ) THEN
        -- Backfill: Set default value for existing NULL records
        UPDATE elevators
        SET label_type = 'BLUE'::label_type
        WHERE label_type IS NULL;
        
        -- Make column NOT NULL
        ALTER TABLE elevators
        ALTER COLUMN label_type SET NOT NULL;
    END IF;
END $$;

-- Ensure label_date is NOT NULL (should already be, but double-check)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'elevators' 
        AND column_name = 'label_date'
        AND is_nullable = 'YES'
    ) THEN
        -- Backfill: Set label_date = inspection_date for NULL records
        UPDATE elevators
        SET label_date = inspection_date
        WHERE label_date IS NULL;
        
        -- Make column NOT NULL
        ALTER TABLE elevators
        ALTER COLUMN label_date SET NOT NULL;
    END IF;
END $$;

-- Ensure expiry_date is NOT NULL (should already be, but double-check)
DO $$ 
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'elevators' 
        AND column_name = 'expiry_date'
        AND is_nullable = 'YES'
    ) THEN
        -- Backfill: Calculate expiry_date from label_date + duration for NULL records
        UPDATE elevators e
        SET expiry_date = CASE
            WHEN e.label_type = 'BLUE'::label_type THEN e.label_date + INTERVAL '12 months'
            WHEN e.label_type = 'GREEN'::label_type THEN e.label_date + INTERVAL '24 months'
            WHEN e.label_type = 'YELLOW'::label_type THEN e.label_date + INTERVAL '6 months'
            WHEN e.label_type = 'RED'::label_type THEN e.label_date + INTERVAL '1 month'
            WHEN e.label_type = 'ORANGE'::label_type THEN e.label_date + INTERVAL '9 months'
            ELSE e.label_date + INTERVAL '12 months'
        END
        WHERE expiry_date IS NULL;
        
        -- Make column NOT NULL
        ALTER TABLE elevators
        ALTER COLUMN expiry_date SET NOT NULL;
    END IF;
END $$;

-- Add constraint: expiry_date must be after label_date
DO $$ 
BEGIN
    -- Drop constraint if exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'elevators_expiry_after_label_date'
    ) THEN
        ALTER TABLE elevators DROP CONSTRAINT elevators_expiry_after_label_date;
    END IF;
    
    -- Add constraint
    ALTER TABLE elevators
    ADD CONSTRAINT elevators_expiry_after_label_date 
    CHECK (expiry_date > label_date);
END $$;

-- Add constraint: manager_tc_identity_no must be exactly 11 digits
DO $$ 
BEGIN
    -- Drop constraint if exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'elevators_manager_tc_format'
    ) THEN
        ALTER TABLE elevators DROP CONSTRAINT elevators_manager_tc_format;
    END IF;
    
    -- Add constraint
    ALTER TABLE elevators
    ADD CONSTRAINT elevators_manager_tc_format 
    CHECK (manager_tc_identity_no ~ '^[0-9]{11}$');
END $$;

-- Add constraint: manager_phone must be 10-11 digits
DO $$ 
BEGIN
    -- Drop constraint if exists
    IF EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'elevators_manager_phone_format'
    ) THEN
        ALTER TABLE elevators DROP CONSTRAINT elevators_manager_phone_format;
    END IF;
    
    -- Add constraint
    ALTER TABLE elevators
    ADD CONSTRAINT elevators_manager_phone_format 
    CHECK (manager_phone ~ '^[0-9]{10,11}$');
END $$;
