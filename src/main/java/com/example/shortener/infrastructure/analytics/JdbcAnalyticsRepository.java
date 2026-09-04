package com.example.shortener.infrastructure.analytics;

import com.example.shortener.domain.analytics.AnalyticsRepository;
import com.example.shortener.domain.analytics.ClickEvent;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

@Repository
public class JdbcAnalyticsRepository implements AnalyticsRepository {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcAnalyticsRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(ClickEvent event) {
        String sql = """
            INSERT INTO analytics (id, link_id, timestamp, anonymized_ip, user_agent)
            SELECT :id, :linkId, :timestamp, :ip, :ua
            WHERE EXISTS (SELECT 1 FROM links WHERE id = :linkId)
            ON CONFLICT (id) DO NOTHING
            """;

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("id", event.eventId())
            .addValue("linkId", event.linkId())
            .addValue("timestamp", event.timestamp().atOffset(ZoneOffset.UTC))
            .addValue("ip", event.anonymizedIp())
            .addValue("ua", event.userAgent());

        jdbcTemplate.update(sql, params);
    }

    @Override
    public List<ClickEvent> findByLinkId(UUID linkId) {
        String sql = "SELECT * FROM analytics WHERE link_id = :linkId ORDER BY timestamp DESC";

        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("linkId", linkId);

        return jdbcTemplate.query(sql, params, (rs, rowNum) -> new ClickEvent(
            UUID.fromString(rs.getString("id")),
            UUID.fromString(rs.getString("link_id")),
            rs.getTimestamp("timestamp").toInstant(),
            rs.getString("anonymized_ip"),
            rs.getString("user_agent")
        ));
    }

    @Override
    public long countByLinkId(UUID linkId) {
        String sql = "SELECT count(*) FROM analytics WHERE link_id = :linkId";
        MapSqlParameterSource params = new MapSqlParameterSource()
            .addValue("linkId", linkId);
        return jdbcTemplate.queryForObject(sql, params, Long.class);
    }
}
