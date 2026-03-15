ALTER TABLE IF EXISTS public.demo_requests
    ADD COLUMN IF NOT EXISTS emailed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE IF EXISTS public.demo_requests
    ADD COLUMN IF NOT EXISTS email_error TEXT;

ALTER TABLE IF EXISTS public.demo_requests
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();

CREATE TABLE IF NOT EXISTS public.trial_requests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    company_size VARCHAR(100),
    tenant_slug VARCHAR(255),
    tenant_schema VARCHAR(255),
    login_url VARCHAR(512),
    temporary_password VARCHAR(255),
    provisioning_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP,
    emailed BOOLEAN NOT NULL DEFAULT FALSE,
    email_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.plan_requests (
    id BIGSERIAL PRIMARY KEY,
    plan_code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255) NOT NULL,
    notified BOOLEAN NOT NULL DEFAULT FALSE,
    notification_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trial_requests_created_at
    ON public.trial_requests(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_trial_requests_email
    ON public.trial_requests(email);

CREATE INDEX IF NOT EXISTS idx_trial_requests_status
    ON public.trial_requests(provisioning_status);

CREATE INDEX IF NOT EXISTS idx_trial_requests_tenant_slug
    ON public.trial_requests(tenant_slug);

CREATE INDEX IF NOT EXISTS idx_plan_requests_created_at
    ON public.plan_requests(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_plan_requests_email
    ON public.plan_requests(email);

CREATE INDEX IF NOT EXISTS idx_plan_requests_plan_code
    ON public.plan_requests(plan_code);
