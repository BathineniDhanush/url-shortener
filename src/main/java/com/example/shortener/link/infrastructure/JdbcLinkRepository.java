package com.example.shortener.link.infrastructure;

import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.domain.OwnedLink;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcLinkRepository implements LinkRepository {
    private static final String INSERT_SQL = """
            INSERT INTO links (id, code, destination_url, status, expires_at, created_at, updated_at,
                               owner_token_hash, version)
            VALUES (:id, :code, :destinationUrl, :status, :expiresAt, :createdAt, :updatedAt,
                    :ownerTokenHash, :version)
            """;
    private static final String FIND_BY_CODE_SQL = """
            SELECT id, code, destination_url, status, expires_at, created_at, updated_at
            FROM links
            WHERE code = :code
            """;
    private static final String FIND_OWNED_BY_CODE_SQL = """
            SELECT id, code, destination_url, status, expires_at, created_at, updated_at,
                   owner_token_hash, version
            FROM links
            WHERE code = :code
            """;
    private static final String UPDATE_SQL = """
            UPDATE links
            SET destination_url = :destinationUrl, status = :status, expires_at = :expiresAt,
                updated_at = :updatedAt, version = version + 1
            WHERE code = :code AND version = :expectedVersion
            """;
    private static final String DELETE_SQL = """
            DELETE FROM links
            WHERE code = :code AND owner_token_hash = :ownerTokenHash AND version = :expectedVersion
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public JdbcLinkRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void insert(OwnedLink ownedLink) {
        Link link = ownedLink.link();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id", link.id())
                .addValue("code", link.code())
                .addValue("destinationUrl", link.destinationUrl())
                .addValue("status", link.status().name())
                .addValue("expiresAt", toOffsetDateTime(link.expiresAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("createdAt", toOffsetDateTime(link.createdAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("updatedAt", toOffsetDateTime(link.updatedAt()), Types.TIMESTAMP_WITH_TIMEZONE);
        parameters.addValue("ownerTokenHash", ownedLink.ownerTokenHash())
                .addValue("version", ownedLink.version());
        jdbcTemplate.update(INSERT_SQL, parameters);
    }

    @Override
    public Optional<OwnedLink> findOwnedByCode(String code) {
        return jdbcTemplate.query(FIND_OWNED_BY_CODE_SQL, Map.of("code", code), resultSet ->
                resultSet.next() ? Optional.of(new OwnedLink(mapLink(resultSet),
                        resultSet.getString("owner_token_hash"), resultSet.getLong("version"))) : Optional.empty());
    }

    @Override
    public boolean update(OwnedLink ownedLink, long expectedVersion) {
        Link link = ownedLink.link();
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("code", link.code())
                .addValue("destinationUrl", link.destinationUrl())
                .addValue("status", link.status().name())
                .addValue("expiresAt", toOffsetDateTime(link.expiresAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("updatedAt", toOffsetDateTime(link.updatedAt()), Types.TIMESTAMP_WITH_TIMEZONE)
                .addValue("expectedVersion", expectedVersion);
        return jdbcTemplate.update(UPDATE_SQL, parameters) == 1;
    }

    @Override
    public Optional<Link> findByCode(String code) {
        return jdbcTemplate.query(FIND_BY_CODE_SQL, Map.of("code", code), resultSet ->
                resultSet.next() ? Optional.of(mapLink(resultSet)) : Optional.empty());
    }

    @Override
    public boolean delete(String code, String ownerTokenHash, long expectedVersion) {
        return jdbcTemplate.update(DELETE_SQL,
                Map.of("code", code, "ownerTokenHash", ownerTokenHash,
                        "expectedVersion", expectedVersion)) == 1;
    }

    private Link mapLink(ResultSet resultSet) throws SQLException {
        return new Link(
                resultSet.getObject("id", UUID.class),
                resultSet.getString("code"),
                resultSet.getString("destination_url"),
                LinkStatus.valueOf(resultSet.getString("status")),
                nullableInstant(resultSet, "expires_at"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant());
    }

    private Instant nullableInstant(ResultSet resultSet, String column) throws SQLException {
        var timestamp = resultSet.getObject(column, OffsetDateTime.class);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant == null ? null : instant.atOffset(ZoneOffset.UTC);
    }
}
