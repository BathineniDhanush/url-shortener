package com.example.shortener.infrastructure.cache;

import com.example.shortener.link.domain.Link;
import com.example.shortener.observability.ApplicationMetrics;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
@Component
public class LinkCache {
    private static final Logger log = LoggerFactory.getLogger(LinkCache.class);
    private static final String CACHE_PREFIX = "link:cache:";
    private static final String NOT_FOUND_MARKER = "NOT_FOUND";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final ApplicationMetrics metrics;

    @Autowired
    public LinkCache(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                     ObjectMapper objectMapper, ApplicationMetrics metrics) {
        this(redisTemplateProvider.getIfAvailable(), objectMapper, metrics);
    }

    public LinkCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, ApplicationMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.metrics = metrics;
    }

    public CacheResult<Link> get(String code) {
        if (redisTemplate == null) {
            metrics.cacheLookup(ApplicationMetrics.CacheOutcome.MISS);
            return CacheResult.miss();
        }
        String key = CACHE_PREFIX + code;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                metrics.cacheLookup(ApplicationMetrics.CacheOutcome.MISS);
                return CacheResult.miss();
            }

            if (NOT_FOUND_MARKER.equals(value)) {
                metrics.cacheLookup(ApplicationMetrics.CacheOutcome.NEGATIVE_HIT);
                log.debug("Cache hit (negative cache) for code: {}", code);
                return CacheResult.negativeHit();
            }

            try {
                Link link = objectMapper.readValue(value, Link.class);
                metrics.cacheLookup(ApplicationMetrics.CacheOutcome.HIT);
                return CacheResult.hit(link);
            } catch (JsonProcessingException e) {
                metrics.cacheLookup(ApplicationMetrics.CacheOutcome.ERROR);
                log.error("Failed to deserialize cached link for code {}: {}", code, e.getMessage());
                return CacheResult.miss();
            }
        } catch (Exception e) {
            metrics.cacheLookup(ApplicationMetrics.CacheOutcome.ERROR);
            log.warn("Redis error during cache get for code {}: {}", code, e.getMessage());
            return CacheResult.miss();
        }
    }

    public void put(String code, Link link, Duration ttl) {
        if (redisTemplate == null) {
            return;
        }
        String key = CACHE_PREFIX + code;
        try {
            String value = objectMapper.writeValueAsString(link);
            redisTemplate.opsForValue().set(key, value, ttl);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize link for caching code {}: {}", code, e.getMessage());
        } catch (Exception e) {
            log.warn("Redis error during cache put for code {}: {}", code, e.getMessage());
        }
    }

    public void putNegative(String code, Duration ttl) {
        if (redisTemplate == null) {
            return;
        }
        String key = CACHE_PREFIX + code;
        try {
            redisTemplate.opsForValue().set(key, NOT_FOUND_MARKER, ttl);
        } catch (Exception e) {
            log.warn("Redis error during negative cache put for code {}: {}", code, e.getMessage());
        }
    }

    public void evict(String code) {
        if (redisTemplate != null) {
            redisTemplate.delete(CACHE_PREFIX + code);
        }
    }
}
