package com.example.shortener.infrastructure.cache;

import com.example.shortener.link.domain.Link;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class LinkCache {
    private static final Logger log = LoggerFactory.getLogger(LinkCache.class);
    private static final String CACHE_PREFIX = "link:cache:";
    private static final String NOT_FOUND_MARKER = "NOT_FOUND";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public LinkCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<Link> get(String code) {
        String key = CACHE_PREFIX + code;
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) {
                return Optional.empty();
            }

            if (NOT_FOUND_MARKER.equals(value)) {
                log.debug("Cache miss (negative cache) for code: {}", code);
                return Optional.empty();
            }

            try {
                return Optional.of(objectMapper.readValue(value, Link.class));
            } catch (JsonProcessingException e) {
                log.error("Failed to deserialize cached link for code {}: {}", code, e.getMessage());
                return Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Redis error during cache get for code {}: {}", code, e.getMessage());
            return Optional.empty();
        }
    }

    public void put(String code, Link link, Duration ttl) {
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
        String key = CACHE_PREFIX + code;
        try {
            redisTemplate.opsForValue().set(key, NOT_FOUND_MARKER, ttl);
        } catch (Exception e) {
            log.warn("Redis error during negative cache put for code {}: {}", code, e.getMessage());
        }
    }

    public void evict(String code) {
        redisTemplate.delete(CACHE_PREFIX + code);
    }
}
