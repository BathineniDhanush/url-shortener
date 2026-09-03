package com.example.shortener.redirect.api;

import com.example.shortener.domain.analytics.ClickEvent;
import com.example.shortener.observability.ApplicationMetrics;
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
    private final ApplicationMetrics metrics;

    public AnalyticsPublisher(StringRedisTemplate redisTemplate, ApplicationMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
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
            metrics.analyticsPublication(ApplicationMetrics.PublicationOutcome.PUBLISHED);
        } catch (Exception e) {
            metrics.analyticsPublication(ApplicationMetrics.PublicationOutcome.FAILED);
            log.warn("Failed to publish click event to Redis: {}", e.getMessage());
        }
    }
}
