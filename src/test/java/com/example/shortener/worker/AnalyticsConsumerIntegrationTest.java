package com.example.shortener.worker;

import com.example.shortener.domain.analytics.AnalyticsRepository;
import com.example.shortener.observability.ApplicationMetrics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@Testcontainers(disabledWithoutDocker = true)
class AnalyticsConsumerIntegrationTest {
    private static final String STREAM_KEY = "clicks:stream";
    private static final String DEAD_LETTER_STREAM_KEY = "clicks:dead-letter";
    private static final String GROUP_NAME = "analytics-group";

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redisTemplate;
    private AnalyticsConsumer consumer;
    private ApplicationMetrics metrics;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();
        redisTemplate = new StringRedisTemplate(connectionFactory);
        redisTemplate.afterPropertiesSet();
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        metrics = mock(ApplicationMetrics.class);
    }

    @AfterEach
    void tearDown() {
        if (consumer != null) {
            consumer.stop();
        }
        connectionFactory.destroy();
    }

    @Test
    void acknowledgesSuccessfullyPersistedRecord() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        consumer = new AnalyticsConsumer(redisTemplate, repository, "success-worker", 3, metrics);
        consumer.run(null);

        redisTemplate.opsForStream().add(STREAM_KEY, validRecord());

        verify(repository, timeout(5_000)).save(any());
        verify(metrics, timeout(5_000)).analyticsConsumerEvent(ApplicationMetrics.ConsumerOutcome.PROCESSED);
        await(() -> pendingCount() == 0, Duration.ofSeconds(5));
        assertEquals(0, pendingCount());
        assertEquals(0, streamSize(DEAD_LETTER_STREAM_KEY));
    }

    @Test
    void retriesPendingRecordThenDeadLettersAndAcknowledgesIt() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        doThrow(new IllegalStateException("database unavailable")).when(repository).save(any());
        consumer = new AnalyticsConsumer(redisTemplate, repository, "retry-worker", 3, metrics);
        consumer.run(null);

        redisTemplate.opsForStream().add(STREAM_KEY, validRecord());

        await(() -> pendingCount() == 1, Duration.ofSeconds(5));
        verify(repository, timeout(12_000).times(3)).save(any());
        verify(metrics, timeout(12_000).times(2))
            .analyticsConsumerEvent(ApplicationMetrics.ConsumerOutcome.RETRY);
        verify(metrics, timeout(12_000))
            .analyticsConsumerEvent(ApplicationMetrics.ConsumerOutcome.DEAD_LETTERED);
        await(() -> streamSize(DEAD_LETTER_STREAM_KEY) == 1 && pendingCount() == 0, Duration.ofSeconds(12));
        assertEquals(1, streamSize(DEAD_LETTER_STREAM_KEY));
        assertEquals(0, pendingCount());
    }

    @Test
    void resumesRetryBudgetPersistedInRedis() {
        AnalyticsRepository repository = mock(AnalyticsRepository.class);
        doThrow(new IllegalStateException("database unavailable")).when(repository).save(any());
        var recordId = redisTemplate.opsForStream().add(STREAM_KEY, validRecord());
        redisTemplate.opsForHash().put("clicks:retry-attempts", recordId.getValue(), "2");

        consumer = new AnalyticsConsumer(redisTemplate, repository, "restart-worker", 3, metrics);
        consumer.run(null);

        await(() -> streamSize(DEAD_LETTER_STREAM_KEY) == 1, Duration.ofSeconds(7));
        assertEquals(0, pendingCount());
        assertTrue(!redisTemplate.opsForHash().hasKey("clicks:retry-attempts", recordId.getValue()));
    }

    private Map<String, String> validRecord() {
        return Map.of(
            "eventId", UUID.randomUUID().toString(),
            "linkId", UUID.randomUUID().toString(),
            "timestamp", Instant.parse("2026-09-02T12:00:00Z").toString(),
            "ip", "192.0.2.0",
            "ua", "integration-test"
        );
    }

    private long pendingCount() {
        var pending = redisTemplate.opsForStream().pending(STREAM_KEY, GROUP_NAME);
        return pending == null ? 0 : pending.getTotalPendingMessages();
    }

    private long streamSize(String stream) {
        Long size = redisTemplate.opsForStream().size(stream);
        return size == null ? 0 : size;
    }

    private void await(BooleanSupplier condition, Duration timeout) {
        Instant deadline = Instant.now().plus(timeout);
        while (Instant.now().isBefore(deadline)) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted while awaiting Redis state", exception);
            }
        }
        assertTrue(condition.getAsBoolean(), "Timed out awaiting Redis state");
    }
}
