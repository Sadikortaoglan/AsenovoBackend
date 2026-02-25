-- Add lifecycle fields to maintenance_plans table
ALTER TABLE maintenance_plans
ADD COLUMN IF NOT EXISTS completed_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS qr_proof_id BIGINT REFERENCES qr_proofs(id) ON DELETE SET NULL,
ADD COLUMN IF NOT EXISTS note TEXT,
ADD COLUMN IF NOT EXISTS price NUMERIC(14,2);

-- Add index for completed_at for faster queries
CREATE INDEX IF NOT EXISTS idx_maintenance_plans_completed_at ON maintenance_plans(completed_at);
CREATE INDEX IF NOT EXISTS idx_maintenance_plans_qr_proof_id ON maintenance_plans(qr_proof_id);

-- Add maintenance_plan_photos table for plan photos (separate from session attachments)
CREATE TABLE IF NOT EXISTS maintenance_plan_photos (
    id BIGSERIAL PRIMARY KEY,
    plan_id BIGINT NOT NULL REFERENCES maintenance_plans(id) ON DELETE CASCADE,
    file_url VARCHAR(500) NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    uploaded_by BIGINT REFERENCES users(id) ON DELETE SET NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_maintenance_plan_photos_plan_id ON maintenance_plan_photos(plan_id);
