CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TABLE IF NOT EXISTS public.revision_standards (
    id BIGSERIAL PRIMARY KEY,
    standard_code VARCHAR(50) NOT NULL,
    article_no VARCHAR(50) NOT NULL,
    description TEXT NOT NULL,
    tag_color VARCHAR(20),
    source_file_name VARCHAR(255),
    source_version VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_revision_standards_standard_article UNIQUE (standard_code, article_no)
);

CREATE INDEX IF NOT EXISTS idx_revision_article
    ON public.revision_standards(article_no);

CREATE INDEX IF NOT EXISTS idx_revision_standard_code
    ON public.revision_standards(standard_code);

CREATE INDEX IF NOT EXISTS idx_revision_description
    ON public.revision_standards
    USING gin(description gin_trgm_ops);
