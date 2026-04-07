ALTER TABLE brands DROP CONSTRAINT IF EXISTS uk_brands_name;
DROP INDEX IF EXISTS uq_brands_name_active;
CREATE UNIQUE INDEX uq_brands_name_active
    ON brands (LOWER(name))
    WHERE active = true;

ALTER TABLE models DROP CONSTRAINT IF EXISTS uk_models_brand_name;
DROP INDEX IF EXISTS uq_models_brand_name_active;
CREATE UNIQUE INDEX uq_models_brand_name_active
    ON models (brand_id, LOWER(name))
    WHERE active = true;

ALTER TABLE stock_units DROP CONSTRAINT IF EXISTS uk_stock_units_name;
ALTER TABLE stock_units DROP CONSTRAINT IF EXISTS uk_stock_units_abbreviation;
DROP INDEX IF EXISTS uq_stock_units_name_active;
DROP INDEX IF EXISTS uq_stock_units_abbreviation_active;
CREATE UNIQUE INDEX uq_stock_units_name_active
    ON stock_units (LOWER(name))
    WHERE active = true;
CREATE UNIQUE INDEX uq_stock_units_abbreviation_active
    ON stock_units (LOWER(abbreviation))
    WHERE active = true;
