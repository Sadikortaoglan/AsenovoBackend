ALTER TABLE IF EXISTS public.trial_requests
    ADD COLUMN IF NOT EXISTS tenant_database VARCHAR(255);

CREATE INDEX IF NOT EXISTS idx_trial_requests_expires_at
    ON public.trial_requests(expires_at);
