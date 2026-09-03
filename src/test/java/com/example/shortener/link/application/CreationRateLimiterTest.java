package com.example.shortener.link.application;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreationRateLimiterTest {
    @Test
    void allowsCreationWhenRedisIsAbsentOrUnavailable() {
        @SuppressWarnings("unchecked")
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(null);
        CreationRateLimiter absent = new CreationRateLimiter(provider, 0, Duration.ZERO);
        assertThatCode(() -> absent.check(null)).doesNotThrowAnyException();

        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        when(redis.execute(any(), any(), any())).thenThrow(new IllegalStateException("offline"));
        CreationRateLimiter unavailable = new CreationRateLimiter(redis, 10, Duration.ofSeconds(30));
        assertThatCode(() -> unavailable.check("192.0.2.1")).doesNotThrowAnyException();
    }
}
