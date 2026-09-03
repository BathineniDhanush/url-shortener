package com.example.shortener.worker;

import com.example.shortener.domain.analytics.ClickEvent;
import com.example.shortener.domain.analytics.AnalyticsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "worker")
public class AnalyticsConsumer implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private static final String STREAM_KEY = "clicks:stream";
    private static final String GROUP_NAME = "analytics-group";

    private final StringRedisTemplate redisTemplate;
    private final AnalyticsRepository analyticsRepository;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public AnalyticsConsumer(StringRedisTemplate redisTemplate, AnalyticsRepository analyticsRepository) {
        this.redisTemplate = redisTemplate;
        this.analyticsRepository = analyticsRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        ensureConsumerGroupExists();

        // Start a separate thread for consuming to avoid blocking the application startup
        new Thread(this::consume).start();
        log.info("Analytics consumer started in background thread.");
    }

    private void ensureConsumerGroupExists() {
        try {
            redisTemplate.opsForStream().createGroup(STREAM_KEY, GROUP_NAME);
        } catch (Exception e) {
            log.debug("Consumer group already exists or stream not yet created: {}", e.getMessage());
        }
    }

    private void consume() {
        while (running.get()) {
            try {
                // Read from group, starting from last delivered message
                var records = redisTemplate.opsForStream().read(
                    org.springframework.data.redis.connection.stream.Consumer.from(GROUP_NAME, "worker-1"),
                    StreamReadOptions.empty().block(Duration.ofSeconds(5)),
                    StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
                );

                if (records != null) {
                    for (var record : records) {
                        processRecord(record);
                        // Acknowledge message
                        redisTemplate.opsForStream().acknowledge(STREAM_KEY, GROUP_NAME, record.getId());
                    }
                }
            } catch (Exception e) {
                log.error("Error consuming analytics stream: {}", e.getMessage());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
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
        } catch (Exception e) {
            log.error("Failed to process click event record {}: {}", record.getId(), e.getMessage());
            // In a real system, we would move this to a dead-letter queue
        }
    }
}
