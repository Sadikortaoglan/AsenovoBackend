-- Extend users for role hierarchy + B2B unit account linkage

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS locked BOOLEAN NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS user_type VARCHAR(32) NOT NULL DEFAULT 'STAFF',
    ADD COLUMN IF NOT EXISTS staff_id BIGINT,
    ADD COLUMN IF NOT EXISTS b2b_unit_id BIGINT REFERENCES b2b_units(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP;

UPDATE users
SET enabled = COALESCE(active, true)
WHERE enabled IS NULL;

-- Relax role check first so legacy -> new role mapping does not violate existing constraints
DO $$
BEGIN
    ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
    ALTER TABLE users
        ADD CONSTRAINT users_role_check
            CHECK (role IN ('PATRON', 'PERSONEL', 'ADMIN', 'SYSTEM_ADMIN', 'STAFF_ADMIN', 'STAFF_USER', 'CARI_USER'));
END $$;

-- Legacy role migration
UPDATE users SET role = 'SYSTEM_ADMIN' WHERE role = 'ADMIN';
UPDATE users SET role = 'STAFF_ADMIN' WHERE role = 'PATRON';
UPDATE users SET role = 'STAFF_USER' WHERE role = 'PERSONEL';

-- user_type alignment
UPDATE users
SET user_type = CASE
    WHEN role = 'SYSTEM_ADMIN' THEN 'SYSTEM_ADMIN'
    WHEN role = 'CARI_USER' THEN 'CARI'
    ELSE 'STAFF'
END;

DO $$
BEGIN
    ALTER TABLE users DROP CONSTRAINT IF EXISTS users_role_check;
    ALTER TABLE users DROP CONSTRAINT IF EXISTS users_user_type_check;

    ALTER TABLE users
        ADD CONSTRAINT users_role_check
            CHECK (role IN ('SYSTEM_ADMIN', 'STAFF_ADMIN', 'STAFF_USER', 'CARI_USER'));

    ALTER TABLE users
        ADD CONSTRAINT users_user_type_check
            CHECK (user_type IN ('SYSTEM_ADMIN', 'STAFF', 'CARI'));
END $$;

CREATE INDEX IF NOT EXISTS idx_users_b2b_unit_id ON users(b2b_unit_id);
CREATE UNIQUE INDEX IF NOT EXISTS uk_users_b2b_unit_active
    ON users(b2b_unit_id)
    WHERE b2b_unit_id IS NOT NULL AND active = true;
