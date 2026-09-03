package com.example.shortener.worker;

import com.example.shortener.domain.analytics.ClickEvent;
import com.example.shortener.domain.analytics.AnalyticsRepository;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "worker")
public class AnalyticsConsumer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private static final String STREAM_KEY = "clicks:stream";
    private static final String DEAD_LETTER_STREAM_KEY = "clicks:dead-letter";
    private static final String GROUP_NAME = "analytics-group";
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(5);

    private final StringRedisTemplate redisTemplate;
    private final AnalyticsRepository analyticsRepository;
    private final String consumerName;
    private final int maxAttempts;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final Map<RecordId, Integer> failureCounts = new ConcurrentHashMap<>();
    private ExecutorService executor;

    public AnalyticsConsumer(StringRedisTemplate redisTemplate, AnalyticsRepository analyticsRepository,
                             @Value("${app.analytics.consumer-name:worker-1}") String consumerName,
                             @Value("${app.analytics.max-attempts:3}") int maxAttempts) {
        this.redisTemplate = redisTemplate;
        this.analyticsRepository = analyticsRepository;
        this.consumerName = consumerName;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        ensureConsumerGroupExists();
        executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "analytics-consumer-" + consumerName);
            thread.setDaemon(false);
            return thread;
        });
        executor.submit(this::consume);
        log.info("Analytics consumer {} started in background thread", consumerName);
    }

    private void ensureConsumerGroupExists() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.from("0-0"), GROUP_NAME);
        } catch (Exception e) {
            log.debug("Consumer group already exists or stream not yet created: {}", e.getMessage());
        }
    }

    private void consume() {
        while (running.get()) {
            try {
                processBatch(ReadOffset.from("0-0"), Duration.ZERO);
                processBatch(ReadOffset.lastConsumed(), READ_TIMEOUT);
            } catch (Exception e) {
                log.error("Error consuming analytics stream: {}", e.getMessage());
                ensureConsumerGroupExists();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private void processBatch(ReadOffset offset, Duration blockTimeout) {
        StreamReadOptions options = StreamReadOptions.empty().count(10);
        if (!blockTimeout.isZero()) {
            options = options.block(blockTimeout);
        }
        var records = redisTemplate.opsForStream().read(
            Consumer.from(GROUP_NAME, consumerName),
            options,
            StreamOffset.create(STREAM_KEY, offset)
        );
        if (records != null) {
            records.forEach(this::processRecord);
        }
    }

    private void processRecord(MapRecord<String, Object, Object> record) {
        var data = record.getValue();
        try {
            var event = new ClickEvent(
                UUID.fromString(String.valueOf(data.get("eventId"))),
                UUID.fromString(String.valueOf(data.get("linkId"))),
                Instant.parse(String.valueOf(data.get("timestamp"))),
                (String) data.get("ip"),
                (String) data.get("ua")
            );
            analyticsRepository.save(event);
            acknowledge(record);
        } catch (Exception e) {
            int attempts = failureCounts.merge(record.getId(), 1, Integer::sum);
            if (attempts >= maxAttempts) {
                moveToDeadLetter(record, e, attempts);
            } else {
                log.warn("Click event {} failed on attempt {}/{} and remains pending: {}",
                    record.getId(), attempts, maxAttempts, e.getMessage());
            }
        }
    }

    private void moveToDeadLetter(MapRecord<String, Object, Object> record, Exception failure, int attempts) {
        try {
            Map<Object, Object> deadLetter = new HashMap<>(record.getValue());
            deadLetter.put("sourceRecordId", record.getId().getValue());
            deadLetter.put("attempts", Integer.toString(attempts));
            deadLetter.put("failure", truncate(failure.getMessage(), 256));
            deadLetter.put("failedAt", Instant.now().toString());
            redisTemplate.opsForStream().add(DEAD_LETTER_STREAM_KEY, deadLetter);
            acknowledge(record);
            log.error("Click event {} moved to dead-letter stream after {} attempts", record.getId(), attempts);
        } catch (Exception deadLetterFailure) {
            log.error("Unable to dead-letter click event {}; it remains pending: {}",
                record.getId(), deadLetterFailure.getMessage());
        }
    }

    private void acknowledge(MapRecord<String, Object, Object> record) {
        redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
        failureCounts.remove(record.getId());
    }

    private String truncate(String value, int maximumLength) {
        String safeValue = value == null ? "unknown" : value;
        return safeValue.length() <= maximumLength ? safeValue : safeValue.substring(0, maximumLength);
    }

    @PreDestroy
    void stop() {
        running.set(false);
        if (executor == null) {
            return;
        }
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(READ_TIMEOUT.plusSeconds(1).toSeconds(), TimeUnit.SECONDS)) {
                log.warn("Analytics consumer {} did not stop within the shutdown timeout", consumerName);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }
}
