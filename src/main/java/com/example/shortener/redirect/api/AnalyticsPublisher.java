package com.example.shortener.redirect.api;

import com.example.shortener.domain.analytics.ClickEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AnalyticsPublisher {
    private static final Logger log = LoggerFactory.getLogger(AnalyticsPublisher.class);
    private static final String STREAM_KEY = "clicks:stream";
    private final StringRedisTemplate redisTemplate;

    public AnalyticsPublisher(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void publish(ClickEvent event) {
        try {
            Map<String, String> eventData = Map.of(
                "eventId", event.eventId().toString(),
                "linkId", event.linkId().toString(),
                "timestamp", event.timestamp().toString(),
                "ip", event.anonymizedIp() != null ? event.anonymizedIp() : "unknown",
                "ua", event.userAgent() != null ? event.userAgent() : "unknown"
            );
            redisTemplate.opsForStream().add(STREAM_KEY, eventData);
        } catch (Exception e) {
            log.warn("Failed to publish click event to Redis: {}", e.getMessage());
        }
    }
}
