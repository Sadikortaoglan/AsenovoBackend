ALTER TABLE elevators
    ADD COLUMN IF NOT EXISTS facility_id BIGINT REFERENCES facilities(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_elevators_facility_id
    ON elevators (facility_id);
