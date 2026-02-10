-- Add inspection_color enum (reuse label_type enum values)
-- Note: We'll use VARCHAR for simplicity, or create a separate enum
-- For consistency with existing pattern, we'll use VARCHAR with CHECK constraint

-- Add inspection_color column to inspections table
ALTER TABLE inspections
ADD COLUMN IF NOT EXISTS inspection_color VARCHAR(50);

-- Add contacted_person_name column to inspections table
ALTER TABLE inspections
ADD COLUMN IF NOT EXISTS contacted_person_name VARCHAR(255);

-- Backfill: Set default inspection_color for existing records
UPDATE inspections
SET inspection_color = 'GREEN'
WHERE inspection_color IS NULL;

-- Make inspection_color NOT NULL after backfill
ALTER TABLE inspections
ALTER COLUMN inspection_color SET NOT NULL,
ALTER COLUMN inspection_color SET DEFAULT 'GREEN';

-- Add CHECK constraint for inspection_color values
ALTER TABLE inspections
ADD CONSTRAINT inspections_inspection_color_check 
CHECK (inspection_color IN ('GREEN', 'YELLOW', 'RED', 'ORANGE'));

-- Add index for inspection_color (optional, for filtering)
CREATE INDEX IF NOT EXISTS idx_inspections_inspection_color ON inspections(inspection_color);
