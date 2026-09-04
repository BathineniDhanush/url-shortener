package com.example.shortener.redirect.api;

import com.example.shortener.domain.analytics.ClickEvent;
import com.example.shortener.observability.ApplicationMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AnalyticsPublisherTest {

    @Test
    void remainsBestEffortWhenRedisIsDisabled() {
        ApplicationMetrics metrics = mock(ApplicationMetrics.class);
        AnalyticsPublisher publisher = new AnalyticsPublisher((StringRedisTemplate) null, metrics);
        ClickEvent event = new ClickEvent(UUID.randomUUID(), UUID.randomUUID(), Instant.now(),
                "192.0.2.0", "test-agent");

        assertThatCode(() -> publisher.publish(event)).doesNotThrowAnyException();
        verify(metrics).analyticsPublication(ApplicationMetrics.PublicationOutcome.FAILED);
    }
}
