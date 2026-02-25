-- Add order columns for Hibernate @OrderColumn support
-- This converts List collections from Bags to Indexed Collections
-- Required to fix MultipleBagFetchException

-- Add sections_order column to maintenance_sections
ALTER TABLE maintenance_sections 
ADD COLUMN IF NOT EXISTS sections_order INTEGER;

-- Add items_order column to maintenance_items
ALTER TABLE maintenance_items 
ADD COLUMN IF NOT EXISTS items_order INTEGER;

-- Initialize order values from sort_order (preserve existing order)
-- Use COALESCE to ensure no NULL values remain
UPDATE maintenance_sections 
SET sections_order = COALESCE(sort_order, 0)
WHERE sections_order IS NULL;

UPDATE maintenance_items 
SET items_order = COALESCE(sort_order, 0)
WHERE items_order IS NULL;

-- Set default value for future inserts
ALTER TABLE maintenance_sections 
ALTER COLUMN sections_order SET DEFAULT 0;

ALTER TABLE maintenance_items 
ALTER COLUMN items_order SET DEFAULT 0;

-- Make columns NOT NULL after initializing all values
-- First, ensure all NULL values are set
UPDATE maintenance_sections 
SET sections_order = 0 
WHERE sections_order IS NULL;

UPDATE maintenance_items 
SET items_order = 0 
WHERE items_order IS NULL;

-- Now add NOT NULL constraint
ALTER TABLE maintenance_sections 
ALTER COLUMN sections_order SET NOT NULL;

ALTER TABLE maintenance_items 
ALTER COLUMN items_order SET NOT NULL;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_maintenance_sections_order 
ON maintenance_sections(template_id, sections_order);

CREATE INDEX IF NOT EXISTS idx_maintenance_items_order 
ON maintenance_items(section_id, items_order);
