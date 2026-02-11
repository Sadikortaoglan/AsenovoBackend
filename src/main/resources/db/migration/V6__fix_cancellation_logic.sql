-- Fix cancellation logic: NOT_PLANNED status, nullable planned_date
-- This migration updates the maintenance_plans table to support proper cancellation

-- 1. Make planned_date nullable (cancelled plans have no date)
ALTER TABLE maintenance_plans
ALTER COLUMN planned_date DROP NOT NULL;

-- 2. Update status CHECK constraint to use NOT_PLANNED instead of CANCELLED
ALTER TABLE maintenance_plans
DROP CONSTRAINT IF EXISTS maintenance_plans_status_check;

ALTER TABLE maintenance_plans
ADD CONSTRAINT maintenance_plans_status_check 
CHECK (status IN ('NOT_PLANNED', 'PLANNED', 'IN_PROGRESS', 'COMPLETED'));

-- 3. Update default status to NOT_PLANNED for new records
ALTER TABLE maintenance_plans
ALTER COLUMN status SET DEFAULT 'NOT_PLANNED';

-- 4. Convert existing CANCELLED records to NOT_PLANNED
-- Also set planned_date = NULL and assigned_technician_id = NULL for cancelled plans
UPDATE maintenance_plans
SET 
    status = 'NOT_PLANNED',
    planned_date = NULL,
    assigned_technician_id = NULL
WHERE status = 'CANCELLED';
