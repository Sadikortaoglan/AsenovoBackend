ALTER TABLE IF EXISTS public.trial_requests
    ADD COLUMN IF NOT EXISTS request_token VARCHAR(64);

ALTER TABLE IF EXISTS public.trial_requests
    ADD COLUMN IF NOT EXISTS provisioning_error TEXT;

UPDATE public.trial_requests
SET request_token = md5(id::text || ':' || created_at::text)
WHERE request_token IS NULL;

ALTER TABLE public.trial_requests
    ALTER COLUMN request_token SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_trial_requests_request_token
    ON public.trial_requests(request_token);
