-- ============================================================
-- V9: Add ADMIN role and maintenance start audit fields
-- ============================================================

-- 1. Add ADMIN to user_role enum (if using enum, otherwise update CHECK constraint)
-- Note: If users.role uses VARCHAR with CHECK constraint, update the constraint
-- If using enum type, add value:
-- ALTER TYPE user_role ADD VALUE 'ADMIN';

-- For VARCHAR with CHECK constraint (current implementation):
-- First, update existing constraint to include ADMIN
DO $$ 
BEGIN
    -- Drop old constraint if exists
    ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
    
    -- Add new constraint with ADMIN
    ALTER TABLE users 
    ADD CONSTRAINT users_role_check 
    CHECK (role IN ('PATRON', 'PERSONEL', 'ADMIN'));
END $$;

-- 2. Add audit fields to maintenance_plans for start tracking
-- Check if table exists before altering (safety check)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'maintenance_plans') THEN
        ALTER TABLE maintenance_plans
        ADD COLUMN IF NOT EXISTS started_remotely BOOLEAN NOT NULL DEFAULT false,
        ADD COLUMN IF NOT EXISTS started_by_role VARCHAR(50),
        ADD COLUMN IF NOT EXISTS started_at TIMESTAMP,
        ADD COLUMN IF NOT EXISTS started_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
        ADD COLUMN IF NOT EXISTS started_from_ip VARCHAR(45);
    END IF;
END $$;

-- 3. Create index for audit queries (only if table exists)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'maintenance_plans') THEN
        CREATE INDEX IF NOT EXISTS idx_maintenance_plans_started_by 
        ON maintenance_plans(started_by_user_id, started_at);
        
        CREATE INDEX IF NOT EXISTS idx_maintenance_plans_started_remotely 
        ON maintenance_plans(started_remotely, started_at);
    END IF;
END $$;
