package com.example.shortener.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.Gauge;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ApplicationMetrics {
    static final String CACHE_LOOKUPS = "url.shortener.cache.lookups";
    static final String ANALYTICS_PUBLICATIONS = "url.shortener.analytics.publications";
    static final String ANALYTICS_CONSUMER_EVENTS = "url.shortener.analytics.consumer.events";
    static final String ANALYTICS_STREAM_LENGTH = "url.shortener.analytics.stream.length";
    static final String ANALYTICS_PENDING = "url.shortener.analytics.consumer.pending";

    private final MeterRegistry meterRegistry;
    private final AtomicLong streamLength = new AtomicLong();
    private final AtomicLong pendingMessages = new AtomicLong();

    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        Gauge.builder(ANALYTICS_STREAM_LENGTH, streamLength, AtomicLong::get).register(meterRegistry);
        Gauge.builder(ANALYTICS_PENDING, pendingMessages, AtomicLong::get).register(meterRegistry);
    }

    public void cacheLookup(CacheOutcome outcome) {
        meterRegistry.counter(CACHE_LOOKUPS, "result", outcome.tagValue()).increment();
    }

    public void analyticsPublication(PublicationOutcome outcome) {
        meterRegistry.counter(ANALYTICS_PUBLICATIONS, "outcome", outcome.tagValue()).increment();
    }

    public void analyticsConsumerEvent(ConsumerOutcome outcome) {
        meterRegistry.counter(ANALYTICS_CONSUMER_EVENTS, "outcome", outcome.tagValue()).increment();
    }

    public void analyticsBacklog(long length, long pending) {
        streamLength.set(Math.max(0, length));
        pendingMessages.set(Math.max(0, pending));
    }

    public enum CacheOutcome {
        HIT("hit"), NEGATIVE_HIT("negative_hit"), MISS("miss"), ERROR("error");

        private final String tagValue;

        CacheOutcome(String tagValue) {
            this.tagValue = tagValue;
        }

        String tagValue() {
            return tagValue;
        }
    }

    public enum PublicationOutcome {
        PUBLISHED("published"), FAILED("failed");

        private final String tagValue;

        PublicationOutcome(String tagValue) {
            this.tagValue = tagValue;
        }

        String tagValue() {
            return tagValue;
        }
    }

    public enum ConsumerOutcome {
        PROCESSED("processed"), RETRY("retry"), DEAD_LETTERED("dead_lettered");

        private final String tagValue;

        ConsumerOutcome(String tagValue) {
            this.tagValue = tagValue;
        }

        String tagValue() {
            return tagValue;
        }
    }
}
