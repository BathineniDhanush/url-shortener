ALTER TABLE links
    ADD COLUMN owner_token_hash CHAR(64),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

CREATE INDEX idx_links_owner_token_hash ON links (owner_token_hash);

COMMENT ON COLUMN links.owner_token_hash IS
    'SHA-256 digest of the capability token. NULL identifies a legacy link created before ownership was introduced.';
