package com.example.shortener.link.application;

import com.example.shortener.link.error.RateLimitExceededException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

@Component
public class CreationRateLimiter {
    private static final Logger log = LoggerFactory.getLogger(CreationRateLimiter.class);
    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>("""
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
            return current
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final int limit;
    private final long windowSeconds;

    @Autowired
    public CreationRateLimiter(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                               @Value("${app.links.creation-rate-limit:20}") int limit,
                               @Value("${app.links.creation-rate-window:60s}") java.time.Duration window) {
        this(redisTemplateProvider.getIfAvailable(), limit, window);
    }

    public CreationRateLimiter(StringRedisTemplate redisTemplate, int limit, java.time.Duration window) {
        this.redisTemplate = redisTemplate;
        this.limit = Math.max(1, limit);
        this.windowSeconds = Math.max(1, window.toSeconds());
    }

    public void check(String remoteAddress) {
        if (redisTemplate == null) {
            return;
        }
        String key = "rate:create:" + digest(remoteAddress == null ? "unknown" : remoteAddress);
        try {
            Long current = redisTemplate.execute(INCREMENT, List.of(key), Long.toString(windowSeconds));
            if (current != null && current > limit) {
                Long ttl = redisTemplate.getExpire(key);
                throw new RateLimitExceededException(ttl == null || ttl < 1 ? windowSeconds : ttl);
            }
        } catch (RateLimitExceededException exception) {
            throw exception;
        } catch (Exception exception) {
            // Availability wins for the prototype; production should pair this with an edge limiter.
            log.warn("Creation rate limiter unavailable; allowing request: {}", exception.getMessage());
        }
    }

    private String digest(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8))).substring(0, 24);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }
}
