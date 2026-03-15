CREATE INDEX IF NOT EXISTS idx_trial_requests_email_company_lookup
    ON public.trial_requests (LOWER(TRIM(email)), LOWER(TRIM(COALESCE(company, ''))), provisioning_status);

CREATE INDEX IF NOT EXISTS idx_trial_requests_phone_lookup
    ON public.trial_requests ((REGEXP_REPLACE(COALESCE(phone, ''), '[^0-9]', '', 'g')), provisioning_status);
