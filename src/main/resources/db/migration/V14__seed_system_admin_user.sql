-- Ensure a default SYSTEM_ADMIN account exists in every migrated schema.
-- Username: system_admin
-- Password: password -> hash corresponds to the existing seed password convention.
INSERT INTO users (
    username,
    password_hash,
    role,
    user_type,
    active,
    enabled,
    locked,
    created_at,
    updated_at
)
VALUES (
    'system_admin',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'SYSTEM_ADMIN',
    'SYSTEM_ADMIN',
    true,
    true,
    false,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
)
ON CONFLICT (username) DO UPDATE
SET
    role = EXCLUDED.role,
    user_type = EXCLUDED.user_type,
    active = true,
    enabled = true,
    locked = false,
    updated_at = CURRENT_TIMESTAMP
WHERE
    users.role IS DISTINCT FROM 'SYSTEM_ADMIN'
    OR users.user_type IS DISTINCT FROM 'SYSTEM_ADMIN'
    OR users.active IS DISTINCT FROM true
    OR users.enabled IS DISTINCT FROM true
    OR users.locked IS DISTINCT FROM false;
