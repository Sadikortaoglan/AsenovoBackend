CREATE TABLE IF NOT EXISTS public.revision_standard_sets (
    id BIGSERIAL PRIMARY KEY,
    standard_code VARCHAR(50) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

INSERT INTO public.revision_standard_sets (standard_code, created_at, updated_at)
SELECT DISTINCT rs.standard_code, NOW(), NOW()
FROM public.revision_standards rs
WHERE rs.standard_code IS NOT NULL
ON CONFLICT (standard_code) DO NOTHING;

ALTER TABLE public.revision_standards
    ADD COLUMN IF NOT EXISTS price NUMERIC(12, 2) NOT NULL DEFAULT 0;
