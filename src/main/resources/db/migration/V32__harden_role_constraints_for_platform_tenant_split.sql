-- Harden role constraints and data normalization for authoritative role model migration.
-- Safe forward-only migration:
-- - keeps legacy compatibility values readable
-- - enforces platform/tenant boundary in DB constraints
-- - normalizes role + user_type data without destructive deletes

DO $$
DECLARE
    schema_name TEXT := current_schema();
BEGIN
    IF to_regclass(format('%I.users', schema_name)) IS NULL THEN
        RETURN;
    END IF;

    EXECUTE format(
            'UPDATE %I.users
                SET role = UPPER(TRIM(role))
              WHERE role IS NOT NULL',
            schema_name
    );

    EXECUTE format(
            'UPDATE %I.users
                SET user_type = UPPER(TRIM(user_type))
              WHERE user_type IS NOT NULL',
            schema_name
    );

    EXECUTE format(
            'UPDATE %I.users
                SET role = CASE
                    WHEN role IN (''ROLE_PLATFORM_ADMIN'', ''PLATFORM_ADMIN'', ''ADMIN'') THEN ''SYSTEM_ADMIN''
                    WHEN role IN (''ROLE_TENANT_ADMIN'', ''TENANT_ADMIN'', ''PATRON'') THEN ''STAFF_ADMIN''
                    WHEN role IN (''ROLE_STAFF_USER'', ''PERSONEL'') THEN ''STAFF_USER''
                    WHEN role = ''ROLE_CARI_USER'' THEN ''CARI_USER''
                    ELSE role
                END
              WHERE role IS NOT NULL',
            schema_name
    );

    IF schema_name <> 'public' THEN
        EXECUTE format(
                'UPDATE %I.users
                    SET role = ''STAFF_ADMIN''
                  WHERE role IN (''SYSTEM_ADMIN'', ''PLATFORM_ADMIN'')',
                schema_name
        );
    END IF;

    EXECUTE format(
            'UPDATE %I.users
                SET user_type = CASE
                    WHEN role IN (''SYSTEM_ADMIN'', ''PLATFORM_ADMIN'') THEN ''SYSTEM_ADMIN''
                    WHEN role = ''CARI_USER'' THEN ''CARI''
                    ELSE ''STAFF''
                END
              WHERE user_type IS NULL OR user_type NOT IN (''SYSTEM_ADMIN'', ''STAFF'', ''CARI'')',
            schema_name
    );

    IF schema_name <> 'public' THEN
        EXECUTE format(
                'UPDATE %I.users
                    SET user_type = ''STAFF''
                  WHERE role IN (''STAFF_ADMIN'', ''TENANT_ADMIN'', ''STAFF_USER'')
                    AND user_type = ''SYSTEM_ADMIN''',
                schema_name
        );
    END IF;

    EXECUTE format(
            'ALTER TABLE %I.users DROP CONSTRAINT IF EXISTS users_role_check',
            schema_name
    );

    IF schema_name = 'public' THEN
        EXECUTE format(
                'ALTER TABLE %I.users
                    ADD CONSTRAINT users_role_check
                    CHECK (role IN (''SYSTEM_ADMIN'', ''PLATFORM_ADMIN'', ''STAFF_ADMIN'', ''TENANT_ADMIN'', ''STAFF_USER'', ''CARI_USER''))',
                schema_name
        );
    ELSE
        EXECUTE format(
                'ALTER TABLE %I.users
                    ADD CONSTRAINT users_role_check
                    CHECK (role IN (''STAFF_ADMIN'', ''TENANT_ADMIN'', ''STAFF_USER'', ''CARI_USER''))',
                schema_name
        );
    END IF;

    EXECUTE format(
            'ALTER TABLE %I.users DROP CONSTRAINT IF EXISTS users_user_type_check',
            schema_name
    );

    EXECUTE format(
            'ALTER TABLE %I.users
                ADD CONSTRAINT users_user_type_check
                CHECK (user_type IN (''SYSTEM_ADMIN'', ''STAFF'', ''CARI''))',
            schema_name
    );
END $$;

DO $$
BEGIN
    IF current_schema() = 'public' THEN
        CREATE TABLE IF NOT EXISTS public.platform_users (
            id BIGSERIAL PRIMARY KEY,
            username VARCHAR(255) NOT NULL,
            password_hash VARCHAR(255) NOT NULL,
            enabled BOOLEAN NOT NULL DEFAULT TRUE,
            locked BOOLEAN NOT NULL DEFAULT FALSE,
            role VARCHAR(64) NOT NULL,
            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
            last_login_at TIMESTAMP NULL
        );

        ALTER TABLE public.platform_users
            DROP CONSTRAINT IF EXISTS ck_platform_users_role;

        ALTER TABLE public.platform_users
            ADD CONSTRAINT ck_platform_users_role
                CHECK (role = 'ROLE_PLATFORM_ADMIN');

        CREATE UNIQUE INDEX IF NOT EXISTS ux_platform_users_username
            ON public.platform_users (username);

        INSERT INTO public.platform_users (
            username,
            password_hash,
            enabled,
            locked,
            role,
            created_at,
            updated_at,
            last_login_at
        )
        SELECT
            u.username,
            u.password_hash,
            COALESCE(u.enabled, TRUE),
            COALESCE(u.locked, FALSE),
            'ROLE_PLATFORM_ADMIN',
            COALESCE(u.created_at, CURRENT_TIMESTAMP),
            CURRENT_TIMESTAMP,
            u.last_login_at
        FROM public.users u
        WHERE UPPER(u.role) IN ('SYSTEM_ADMIN', 'PLATFORM_ADMIN')
        ON CONFLICT (username) DO UPDATE
        SET
            password_hash = EXCLUDED.password_hash,
            enabled = EXCLUDED.enabled,
            locked = EXCLUDED.locked,
            role = EXCLUDED.role,
            updated_at = CURRENT_TIMESTAMP,
            last_login_at = EXCLUDED.last_login_at;
    END IF;
END $$;
