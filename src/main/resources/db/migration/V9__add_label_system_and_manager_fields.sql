-- Add label_type enum
CREATE TYPE label_type AS ENUM ('GREEN', 'BLUE', 'YELLOW', 'RED');

-- Add label_date and label_type to elevators
-- Rename inspection_date to label_date for clarity
-- Keep expiry_date as end_date (already exists, just ensure it's calculated correctly)
ALTER TABLE elevators
ADD COLUMN IF NOT EXISTS label_date DATE,
ADD COLUMN IF NOT EXISTS label_type label_type;

-- Backfill: Set label_date = inspection_date if label_date is null
UPDATE elevators
SET label_date = inspection_date
WHERE label_date IS NULL;

-- Backfill: Set label_type = 'BLUE' if blue_label = true, otherwise 'GREEN'
-- If blue_label is NULL, default to 'BLUE'
UPDATE elevators
SET label_type = CASE 
    WHEN blue_label = true THEN 'BLUE'::label_type
    WHEN blue_label = false THEN 'GREEN'::label_type
    ELSE 'BLUE'::label_type  -- Default to BLUE if blue_label is NULL
END
WHERE label_type IS NULL;

-- Make label_date NOT NULL after backfill
ALTER TABLE elevators
ALTER COLUMN label_date SET NOT NULL;

-- Set label_type NOT NULL and DEFAULT
ALTER TABLE elevators
ALTER COLUMN label_type SET NOT NULL,
ALTER COLUMN label_type SET DEFAULT 'BLUE'::label_type;

-- Add manager fields to elevators
ALTER TABLE elevators
ADD COLUMN IF NOT EXISTS manager_name VARCHAR(255),
ADD COLUMN IF NOT EXISTS manager_tc_identity_no VARCHAR(11),
ADD COLUMN IF NOT EXISTS manager_phone VARCHAR(20),
ADD COLUMN IF NOT EXISTS manager_email VARCHAR(255);

-- Add label_type to maintenances
ALTER TABLE maintenances
ADD COLUMN IF NOT EXISTS label_type label_type;

-- Backfill maintenance label_type from elevator's current label_type
UPDATE maintenances m
SET label_type = e.label_type
FROM elevators e
WHERE m.elevator_id = e.id
  AND m.label_type IS NULL;

-- Set default for new maintenances
ALTER TABLE maintenances
ALTER COLUMN label_type SET DEFAULT 'BLUE'::label_type;

-- Add status enum and column to elevators
CREATE TYPE elevator_status AS ENUM ('ACTIVE', 'EXPIRED');

ALTER TABLE elevators
ADD COLUMN IF NOT EXISTS status elevator_status DEFAULT 'ACTIVE';

-- Calculate status based on end_date (expiry_date)
UPDATE elevators
SET status = CASE
    WHEN expiry_date < CURRENT_DATE THEN 'EXPIRED'::elevator_status
    ELSE 'ACTIVE'::elevator_status
END;

-- Make status NOT NULL
ALTER TABLE elevators
ALTER COLUMN status SET NOT NULL;

-- Create index for status queries
CREATE INDEX IF NOT EXISTS idx_elevators_status_end_date ON elevators(status, expiry_date);
