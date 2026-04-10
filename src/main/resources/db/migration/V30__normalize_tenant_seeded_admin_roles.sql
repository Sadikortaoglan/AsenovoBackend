-- Keep platform user authority in public schema, but remove platform-level role leakage from tenant schemas.
-- Tenant schemas must not keep SYSTEM_ADMIN-style users.

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
                 SET role = ''STAFF_ADMIN'',
                     user_type = ''STAFF''
                 WHERE role = ''SYSTEM_ADMIN''',
                schema_name
        );
    END IF;
END $$;
