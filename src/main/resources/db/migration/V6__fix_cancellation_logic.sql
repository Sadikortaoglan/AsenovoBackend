-- Fix cancellation logic: NOT_PLANNED status, nullable planned_date
-- This migration updates the maintenance_plans table to support proper cancellation

-- 1. Make planned_date nullable (cancelled plans have no date)
ALTER TABLE maintenance_plans
ALTER COLUMN planned_date DROP NOT NULL;

-- 2. Drop old constraint (PostgreSQL auto-names inline CHECK constraints)
-- Find and drop the constraint on status column
DO $$
DECLARE
    constraint_name TEXT;
BEGIN
    -- Find constraint that checks status column
    SELECT conname INTO constraint_name
    FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY(c.conkey)
    WHERE c.conrelid = 'maintenance_plans'::regclass
    AND c.contype = 'c'
    AND a.attname = 'status'
    LIMIT 1;
    
    IF constraint_name IS NOT NULL THEN
        EXECUTE 'ALTER TABLE maintenance_plans DROP CONSTRAINT ' || quote_ident(constraint_name);
    END IF;
END $$;

-- 3. Convert existing CANCELLED records to NOT_PLANNED (no constraint blocking now)
UPDATE maintenance_plans
SET 
    status = 'NOT_PLANNED',
    planned_date = NULL,
    assigned_technician_id = NULL
WHERE status = 'CANCELLED';

-- 4. Add new CHECK constraint with NOT_PLANNED (data is already updated)
ALTER TABLE maintenance_plans
ADD CONSTRAINT maintenance_plans_status_check 
CHECK (status IN ('NOT_PLANNED', 'PLANNED', 'IN_PROGRESS', 'COMPLETED'));

-- 5. Update default status to NOT_PLANNED for new records
ALTER TABLE maintenance_plans
ALTER COLUMN status SET DEFAULT 'NOT_PLANNED';
