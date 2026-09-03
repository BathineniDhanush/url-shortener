package com.example.shortener.observability;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class ApplicationMetrics {
    static final String CACHE_LOOKUPS = "url.shortener.cache.lookups";
    static final String ANALYTICS_PUBLICATIONS = "url.shortener.analytics.publications";
    static final String ANALYTICS_CONSUMER_EVENTS = "url.shortener.analytics.consumer.events";

    private final MeterRegistry meterRegistry;

    public ApplicationMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
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
