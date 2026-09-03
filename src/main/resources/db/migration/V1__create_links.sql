CREATE TABLE links (
    id UUID PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    destination_url TEXT NOT NULL,
    status VARCHAR(16) NOT NULL,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_links_code_length CHECK (char_length(code) BETWEEN 4 AND 32),
    CONSTRAINT chk_links_status CHECK (status IN ('ACTIVE', 'DISABLED'))
);

CREATE INDEX idx_links_active_expiration
    ON links (expires_at)
    WHERE status = 'ACTIVE' AND expires_at IS NOT NULL;
