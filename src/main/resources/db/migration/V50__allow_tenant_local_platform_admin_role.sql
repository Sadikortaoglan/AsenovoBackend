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
