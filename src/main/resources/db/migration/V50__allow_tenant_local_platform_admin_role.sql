-- Allow tenant-local PLATFORM_ADMIN role in tenant schemas for local platform admin model.
-- Public schema keeps broader control-plane role constraint as-is.

DO $$
DECLARE
    schema_name TEXT := current_schema();
BEGIN
    IF to_regclass(format('%I.users', schema_name)) IS NULL THEN
        RETURN;
    END IF;

    IF schema_name <> 'public' THEN
        EXECUTE format(
                'UPDATE %I.users
                    SET role = UPPER(TRIM(role))
                  WHERE role IS NOT NULL',
                schema_name
        );

        EXECUTE format(
                'UPDATE %I.users
                    SET role = CASE
                        WHEN role IN (''ROLE_PLATFORM_ADMIN'', ''PLATFORM_ADMIN'', ''SYSTEM_ADMIN'', ''ADMIN'') THEN ''PLATFORM_ADMIN''
                        WHEN role IN (''ROLE_TENANT_ADMIN'', ''TENANT_ADMIN'') THEN ''TENANT_ADMIN''
                        WHEN role IN (''ROLE_STAFF_ADMIN'', ''STAFF_ADMIN'', ''PATRON'') THEN ''STAFF_ADMIN''
                        WHEN role IN (''ROLE_STAFF_USER'', ''STAFF_USER'', ''PERSONEL'') THEN ''STAFF_USER''
                        WHEN role IN (''ROLE_CARI_USER'', ''CARI_USER'') THEN ''CARI_USER''
                        ELSE role
                    END
                  WHERE role IS NOT NULL',
                schema_name
        );

        EXECUTE format(
                'ALTER TABLE %I.users DROP CONSTRAINT IF EXISTS users_role_check',
                schema_name
        );

        EXECUTE format(
                'ALTER TABLE %I.users
                    ADD CONSTRAINT users_role_check
                    CHECK (role IN (''PLATFORM_ADMIN'', ''STAFF_ADMIN'', ''TENANT_ADMIN'', ''STAFF_USER'', ''CARI_USER''))',
                schema_name
        );
    END IF;
END $$;
