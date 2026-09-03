package com.example.shortener.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApplicationMetricsTest {

    @Test
    void recordsStableLowCardinalityMetricNamesAndTags() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ApplicationMetrics metrics = new ApplicationMetrics(registry);

        metrics.cacheLookup(ApplicationMetrics.CacheOutcome.HIT);
        metrics.cacheLookup(ApplicationMetrics.CacheOutcome.NEGATIVE_HIT);
        metrics.cacheLookup(ApplicationMetrics.CacheOutcome.MISS);
        metrics.cacheLookup(ApplicationMetrics.CacheOutcome.ERROR);
        metrics.analyticsPublication(ApplicationMetrics.PublicationOutcome.PUBLISHED);
        metrics.analyticsPublication(ApplicationMetrics.PublicationOutcome.FAILED);
        metrics.analyticsConsumerEvent(ApplicationMetrics.ConsumerOutcome.PROCESSED);
        metrics.analyticsConsumerEvent(ApplicationMetrics.ConsumerOutcome.RETRY);
        metrics.analyticsConsumerEvent(ApplicationMetrics.ConsumerOutcome.DEAD_LETTERED);

        assertEquals(Set.of("hit", "negative_hit", "miss", "error"),
            tagValues(registry, ApplicationMetrics.CACHE_LOOKUPS, "result"));
        assertEquals(Set.of("published", "failed"),
            tagValues(registry, ApplicationMetrics.ANALYTICS_PUBLICATIONS, "outcome"));
        assertEquals(Set.of("processed", "retry", "dead_lettered"),
            tagValues(registry, ApplicationMetrics.ANALYTICS_CONSUMER_EVENTS, "outcome"));
        assertEquals(1.0,
            registry.get(ApplicationMetrics.CACHE_LOOKUPS).tag("result", "hit").counter().count());
    }

    private Set<String> tagValues(SimpleMeterRegistry registry, String metricName, String tagName) {
        return registry.find(metricName).counters().stream()
            .map(counter -> counter.getId().getTag(tagName))
            .collect(Collectors.toSet());
    }
}
