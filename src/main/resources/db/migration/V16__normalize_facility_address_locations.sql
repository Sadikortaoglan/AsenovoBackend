CREATE TABLE IF NOT EXISTS cities (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS districts (
    id BIGSERIAL PRIMARY KEY,
    city_id BIGINT NOT NULL REFERENCES cities(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_districts_city_name UNIQUE (city_id, name)
);

CREATE TABLE IF NOT EXISTS neighborhoods (
    id BIGSERIAL PRIMARY KEY,
    district_id BIGINT NOT NULL REFERENCES districts(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_neighborhoods_district_name UNIQUE (district_id, name)
);

CREATE TABLE IF NOT EXISTS regions (
    id BIGSERIAL PRIMARY KEY,
    neighborhood_id BIGINT NOT NULL REFERENCES neighborhoods(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    CONSTRAINT uk_regions_neighborhood_name UNIQUE (neighborhood_id, name)
);

ALTER TABLE facilities
    ADD COLUMN IF NOT EXISTS city_id BIGINT REFERENCES cities(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS district_id BIGINT REFERENCES districts(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS neighborhood_id BIGINT REFERENCES neighborhoods(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS region_id BIGINT REFERENCES regions(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_facilities_city_id ON facilities(city_id);
CREATE INDEX IF NOT EXISTS idx_facilities_district_id ON facilities(district_id);
CREATE INDEX IF NOT EXISTS idx_facilities_neighborhood_id ON facilities(neighborhood_id);
CREATE INDEX IF NOT EXISTS idx_facilities_region_id ON facilities(region_id);

ALTER TABLE facilities
    DROP COLUMN IF EXISTS city,
    DROP COLUMN IF EXISTS district,
    DROP COLUMN IF EXISTS neighborhood,
    DROP COLUMN IF EXISTS region;
