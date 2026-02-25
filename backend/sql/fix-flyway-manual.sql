-- Flyway Repair - Manual SQL Script
-- Run this script if repair-on-migrate doesn't work

-- Step 1: Delete V5 migration record (if exists)
DELETE FROM flyway_schema_history WHERE version = 5;

-- Step 2: Verify current migration state
SELECT version, description, type, installed_on, success 
FROM flyway_schema_history 
ORDER BY version;

-- Expected result after repair:
-- V1, V2, V3 should be present
-- V4 will be applied on next application start
