DO $$
BEGIN
    -- Do not mutate control-plane schema.
    IF current_schema() = 'public' THEN
        RETURN;
    END IF;

    -- Backward-safe: some schemas may not have the elevator enum yet.
    IF to_regtype('elevator_status') IS NULL THEN
        RETURN;
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_enum e
        JOIN pg_type t ON t.oid = e.enumtypid
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE n.nspname = current_schema()
          AND t.typname = 'elevator_status'
          AND e.enumlabel = 'DELETED'
    ) THEN
        EXECUTE format('ALTER TYPE %I.elevator_status ADD VALUE ''DELETED''', current_schema());
    END IF;
END $$;
