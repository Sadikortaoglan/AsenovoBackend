CREATE TABLE IF NOT EXISTS public.demo_requests (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255) NOT NULL,
    company_size VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS public.contact_messages (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    company VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    emailed BOOLEAN NOT NULL DEFAULT FALSE,
    email_error TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_demo_requests_created_at
    ON public.demo_requests(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_demo_requests_email
    ON public.demo_requests(email);

CREATE INDEX IF NOT EXISTS idx_contact_messages_created_at
    ON public.contact_messages(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_contact_messages_email
    ON public.contact_messages(email);
