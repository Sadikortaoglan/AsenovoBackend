-- ============================================================
-- COMPLETE DATABASE RESET SCRIPT
-- ============================================================
-- WARNING: This will DELETE ALL DATA
-- Use ONLY in development environment
-- ============================================================

-- 1. Disconnect all sessions
SELECT pg_terminate_backend(pg_stat_activity.pid)
FROM pg_stat_activity
WHERE pg_stat_activity.datname = 'sara_asansor'
  AND pid <> pg_backend_pid();

-- 2. Drop schema completely
DROP SCHEMA IF EXISTS public CASCADE;

-- 3. Recreate schema
CREATE SCHEMA public;

-- 4. Grant permissions
GRANT ALL ON SCHEMA public TO sara_asansor;
GRANT ALL ON SCHEMA public TO public;

-- ============================================================
-- Database is now clean and ready for migration
-- ============================================================
