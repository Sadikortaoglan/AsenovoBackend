-- ============================================================
-- V2: BASE SEED DATA (Production-safe)
-- ============================================================
-- Only essential data: 1 PATRON user required by business rules
-- ============================================================

-- 1. USERS (1 PATRON - required by business rules)
INSERT INTO users (username, password_hash, role, active, created_at)
VALUES
    ('patron', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', 'PATRON', true, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;
