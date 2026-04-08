DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM warehouses
        WHERE active = true
        GROUP BY LOWER(name)
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'V59 blocked: active duplicate warehouse names detected (case-insensitive). Resolve duplicates before migration.';
    END IF;
END $$;

DROP INDEX IF EXISTS uq_warehouses_name_active;
CREATE UNIQUE INDEX uq_warehouses_name_active
    ON warehouses (LOWER(name))
    WHERE active = true;
