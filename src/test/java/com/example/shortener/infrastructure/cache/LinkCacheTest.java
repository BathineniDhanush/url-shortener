package com.example.shortener.infrastructure.cache;

import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.observability.ApplicationMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkCacheTest {
    private final ApplicationMetrics metrics = mock(ApplicationMetrics.class);

    @Test
    void treatsRedisReadFailureAndCorruptJsonAsCacheMisses() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("link:cache:redis-down")).thenThrow(new IllegalStateException("offline"));
        when(values.get("link:cache:corrupt")).thenReturn("not-json");
        LinkCache cache = new LinkCache(redis, new ObjectMapper(), metrics);

        assertThat(cache.get("redis-down").isMiss()).isTrue();
        assertThat(cache.get("corrupt").isMiss()).isTrue();
        verify(metrics, org.mockito.Mockito.times(2))
                .cacheLookup(ApplicationMetrics.CacheOutcome.ERROR);
    }

    @Test
    void handlesAllCacheStatesAndEviction() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        Link link = link();
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        when(values.get("link:cache:hit")).thenReturn(mapper.writeValueAsString(link));
        when(values.get("link:cache:negative")).thenReturn("NOT_FOUND");
        when(values.get("link:cache:miss")).thenReturn(null);
        LinkCache cache = new LinkCache(redis, mapper, metrics);

        assertThat(cache.get("hit").getValue()).isEqualTo(link);
        assertThat(cache.get("negative").isNegativeHit()).isTrue();
        assertThat(cache.get("miss").isMiss()).isTrue();
        cache.put("hit", link, Duration.ofMinutes(1));
        cache.putNegative("missing", Duration.ofSeconds(10));
        cache.evict("hit");

        verify(values).set(eq("link:cache:hit"), any(String.class), eq(Duration.ofMinutes(1)));
        verify(values).set("link:cache:missing", "NOT_FOUND", Duration.ofSeconds(10));
        verify(redis).delete("link:cache:hit");
    }

    @Test
    void cacheWritesRemainBestEffort() throws Exception {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        doThrow(new IllegalStateException("offline"))
                .when(values).set(eq("link:cache:write-error"), any(String.class), any(Duration.class));
        doThrow(new IllegalStateException("offline"))
                .when(values).set("link:cache:negative-error", "NOT_FOUND", Duration.ofSeconds(10));
        LinkCache cache = new LinkCache(redis, new ObjectMapper().findAndRegisterModules(), metrics);

        assertThatCode(() -> cache.put("write-error", link(), Duration.ofSeconds(10)))
                .doesNotThrowAnyException();
        assertThatCode(() -> cache.putNegative("negative-error", Duration.ofSeconds(10)))
                .doesNotThrowAnyException();

        ObjectMapper brokenMapper = mock(ObjectMapper.class);
        when(brokenMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("broken") { });
        assertThatCode(() -> new LinkCache(redis, brokenMapper, metrics)
                .put("serialization", link(), Duration.ofSeconds(10))).doesNotThrowAnyException();
    }

    private Link link() {
        Instant now = Instant.parse("2026-09-03T12:00:00Z");
        return new Link(UUID.fromString("c90cbb8d-1f08-4f4e-b8ba-145aa05e342c"), "cache-link",
                "https://example.com", LinkStatus.ACTIVE, null, now, now);
    }
}
