CREATE TABLE analytics (
    id UUID PRIMARY KEY,
    link_id UUID NOT NULL,
    timestamp TIMESTAMPTZ NOT NULL,
    anonymized_ip TEXT,
    user_agent TEXT,
    CONSTRAINT fk_analytics_link FOREIGN KEY (link_id) REFERENCES links (id)
);
CREATE INDEX idx_analytics_link_id ON analytics (link_id);
CREATE INDEX idx_analytics_timestamp ON analytics (timestamp);
