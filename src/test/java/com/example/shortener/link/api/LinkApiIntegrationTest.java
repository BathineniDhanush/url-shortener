package com.example.shortener.link.api;

import com.example.shortener.domain.analytics.AnalyticsRepository;
import com.example.shortener.domain.analytics.ClickEvent;
import com.example.shortener.link.application.ManageLinkService;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.error.ConcurrentLinkUpdateException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.RedisConnection;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers(disabledWithoutDocker = true)
class LinkApiIntegrationTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine");

    @Container
    @ServiceConnection
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private AnalyticsRepository analyticsRepository;

    @Autowired
    private ManageLinkService manageLinkService;

    @BeforeEach
    void clearState() {
        jdbcTemplate.update("DELETE FROM analytics");
        jdbcTemplate.update("DELETE FROM links");
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            connection.serverCommands().flushDb();
        }
    }

    @Test
    void createsAndResolvesGeneratedShortLink() throws Exception {
        String body = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationUrl":"https://example.com/products/42"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.code").value(org.hamcrest.Matchers.matchesPattern("[A-Za-z0-9]{10}")))
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        String code = response.get("code").asText();

        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/products/42"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"));
    }

    @Test
    void returnsConflictWhenCustomAliasAlreadyExists() throws Exception {
        String request = """
                {"destinationUrl":"https://example.com","customAlias":"launch-42"}
                """;
        mockMvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/links").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void rejectsUnsafeDestinationAndExpiredLink() throws Exception {
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationUrl":"javascript:alert(1)"}
                                """))
                .andExpect(status().isBadRequest());

        String expiredRequest = objectMapper.createObjectNode()
                .put("destinationUrl", "https://example.com")
                .put("expiresAt", Instant.now().minusSeconds(60).toString())
                .toString();
        mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(expiredRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsNotFoundForUnknownCode() throws Exception {
        mockMvc.perform(get("/missing1"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void verifiesRedirectCachingAndAnalyticsPublishing() throws Exception {
        // 1. Create a link
        String body = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationUrl":"https://example.com/cache-test"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        JsonNode response = objectMapper.readTree(body);
        String code = response.get("code").asText();

        // 2. First redirect: Should hit DB and publish event
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound());

        // Verify analytics event published to Redis stream
        var streamInfo = redisTemplate.opsForStream().size("clicks:stream");
        org.junit.jupiter.api.Assertions.assertEquals(1L, streamInfo);

        // 3. Second redirect: Should hit cache
        // We can't easily check "hit cache" vs "hit DB" without mocks,
        // but we can verify it still works and publishes another event.
        mockMvc.perform(get("/" + code))
                .andExpect(status().isFound());

        var streamInfoAfter = redisTemplate.opsForStream().size("clicks:stream");
        org.junit.jupiter.api.Assertions.assertEquals(2L, streamInfoAfter);
    }

    @Test
    void persistsAnalyticsTimestampIdempotentlyAndCountsInDatabase() throws Exception {
        mockMvc.perform(post("/api/v1/links")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"destinationUrl":"https://example.com/analytics-test","customAlias":"analytics-test"}
                        """))
            .andExpect(status().isCreated());

        UUID linkId = jdbcTemplate.queryForObject(
            "SELECT id FROM links WHERE code = ?", UUID.class, "analytics-test");
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.parse("2026-09-02T12:34:56.123456Z");
        ClickEvent event = new ClickEvent(eventId, linkId, occurredAt, "192.0.2.0", "test-agent");

        analyticsRepository.save(event);
        analyticsRepository.save(event);

        org.junit.jupiter.api.Assertions.assertEquals(1L, analyticsRepository.countByLinkId(linkId));
        ClickEvent persisted = analyticsRepository.findByLinkId(linkId).get(0);
        org.junit.jupiter.api.Assertions.assertEquals(eventId, persisted.eventId());
        org.junit.jupiter.api.Assertions.assertEquals(occurredAt, persisted.timestamp());
    }

    @Test
    void ownerCanReadUpdateDisableAndEnableWhileUnauthorizedRequestsAreRejected() throws Exception {
        String createdBody = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationUrl":"https://example.com/old","customAlias":"owned-link"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.ownerToken").isString())
                .andExpect(jsonPath("$.version").value(0))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(createdBody).get("ownerToken").asText();

        mockMvc.perform(get("/api/v1/links/owned-link"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/links/owned-link")
                        .header("X-Link-Owner-Token", "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/links/owned-link")
                        .header("X-Link-Owner-Token", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerToken").doesNotExist());

        mockMvc.perform(get("/owned-link")).andExpect(status().isFound());
        mockMvc.perform(patch("/api/v1/links/owned-link")
                        .header("X-Link-Owner-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion":0,"destinationUrl":"https://example.com/new","status":"DISABLED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));
        mockMvc.perform(get("/owned-link")).andExpect(status().isGone());

        mockMvc.perform(patch("/api/v1/links/owned-link")
                        .header("X-Link-Owner-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":0,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(patch("/api/v1/links/owned-link")
                        .header("X-Link-Owner-Token", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"expectedVersion\":1,\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        mockMvc.perform(get("/owned-link"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://example.com/new"));

        mockMvc.perform(get("/api/v1/links/owned-link/analytics"))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/links/owned-link/analytics")
                        .header("X-Link-Owner-Token", token))
                .andExpect(status().isOk());
    }

    @Test
    void enforcesAtomicRedisBackedCreationRateLimit() {
        var limiter = new com.example.shortener.link.application.CreationRateLimiter(
                redisTemplate, 2, java.time.Duration.ofSeconds(30));
        limiter.check("192.0.2.10");
        limiter.check("192.0.2.10");
        org.junit.jupiter.api.Assertions.assertThrows(
                com.example.shortener.link.error.RateLimitExceededException.class,
                () -> limiter.check("192.0.2.10"));
        limiter.check("192.0.2.11");
    }

    @Test
    void allowsOnlyOneConcurrentUpdateForTheSameVersion() throws Exception {
        String body = mockMvc.perform(post("/api/v1/links")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"destinationUrl":"https://example.com/start","customAlias":"concurrent-link"}
                                """))
                .andReturn().getResponse().getContentAsString();
        String token = objectMapper.readTree(body).get("ownerToken").asText();
        var start = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.Callable<Boolean> update = () -> {
            start.await();
            try {
                manageLinkService.update("concurrent-link", token, 0,
                        "https://example.com/updated", LinkStatus.ACTIVE, null);
                return true;
            } catch (ConcurrentLinkUpdateException exception) {
                return false;
            }
        };
        var first = executor.submit(update);
        var second = executor.submit(update);
        start.countDown();
        try {
            assertEquals(1, java.util.stream.Stream.of(first.get(), second.get())
                    .filter(Boolean::booleanValue).count());
            assertEquals(1L, jdbcTemplate.queryForObject(
                    "SELECT version FROM links WHERE code = 'concurrent-link'", Long.class));
        } finally {
            executor.shutdownNow();
        }
    }
}
