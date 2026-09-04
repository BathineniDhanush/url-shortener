ALTER TABLE analytics
    DROP CONSTRAINT fk_analytics_link;

ALTER TABLE analytics
    ADD CONSTRAINT fk_analytics_link
        FOREIGN KEY (link_id) REFERENCES links (id) ON DELETE CASCADE;
