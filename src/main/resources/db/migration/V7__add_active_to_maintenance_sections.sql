-- Add active field to maintenance_sections table
ALTER TABLE maintenance_sections
ADD COLUMN IF NOT EXISTS active BOOLEAN NOT NULL DEFAULT true;

CREATE INDEX IF NOT EXISTS idx_maintenance_sections_active ON maintenance_sections(active);
