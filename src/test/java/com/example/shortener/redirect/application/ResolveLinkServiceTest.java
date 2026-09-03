package com.example.shortener.redirect.application;

import com.example.shortener.infrastructure.cache.CacheResult;
import com.example.shortener.infrastructure.cache.LinkCache;
import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.error.LinkNotFoundException;
import com.example.shortener.link.error.LinkUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ResolveLinkServiceTest {
    private static final Instant NOW = Instant.parse("2026-09-02T12:00:00Z");

    private LinkRepository repository;
    private LinkCache cache;
    private ResolveLinkService service;

    @BeforeEach
    void setUp() {
        repository = mock(LinkRepository.class);
        cache = mock(LinkCache.class);
        service = new ResolveLinkService(repository, Clock.fixed(NOW, ZoneOffset.UTC), cache);
    }

    @Test
    void returnsActiveUnexpiredPositiveCacheHitWithoutDatabaseLookup() {
        Link link = link(LinkStatus.ACTIVE, NOW.plusSeconds(60));
        when(cache.get("cached")).thenReturn(CacheResult.hit(link));

        assertEquals(link, service.resolve("cached"));

        verifyNoInteractions(repository);
        verify(cache, never()).evict("cached");
    }

    @Test
    void negativeCacheHitReturnsNotFoundWithoutDatabaseLookup() {
        when(cache.get("missing")).thenReturn(CacheResult.negativeHit());

        assertThrows(LinkNotFoundException.class, () -> service.resolve("missing"));

        verifyNoInteractions(repository);
        verify(cache, never()).putNegative("missing", Duration.ofMinutes(5));
    }

    @Test
    void databaseMissCreatesFiveMinuteNegativeCacheEntry() {
        when(cache.get("missing")).thenReturn(CacheResult.miss());
        when(repository.findByCode("missing")).thenReturn(Optional.empty());

        assertThrows(LinkNotFoundException.class, () -> service.resolve("missing"));

        verify(cache).putNegative("missing", Duration.ofMinutes(5));
    }

    @Test
    void expiredCachedLinkIsEvictedAndRejected() {
        Link link = link(LinkStatus.ACTIVE, NOW);
        when(cache.get("expired")).thenReturn(CacheResult.hit(link));
        when(repository.findByCode("expired")).thenReturn(Optional.of(link));

        assertThrows(LinkUnavailableException.class, () -> service.resolve("expired"));

        verify(cache).evict("expired");
        verify(cache, never()).put("expired", link, Duration.ofHours(1));
    }

    @Test
    void disabledCachedLinkIsEvictedAndRejected() {
        Link link = link(LinkStatus.DISABLED, null);
        when(cache.get("disabled")).thenReturn(CacheResult.hit(link));
        when(repository.findByCode("disabled")).thenReturn(Optional.of(link));

        assertThrows(LinkUnavailableException.class, () -> service.resolve("disabled"));

        verify(cache).evict("disabled");
        verify(cache, never()).put("disabled", link, Duration.ofHours(1));
    }

    @Test
    void positiveCacheTtlIsCappedAtRemainingLinkLifetime() {
        Link link = link(LinkStatus.ACTIVE, NOW.plus(Duration.ofMinutes(20)));
        when(cache.get("short-lived")).thenReturn(CacheResult.miss());
        when(repository.findByCode("short-lived")).thenReturn(Optional.of(link));
        ArgumentCaptor<Duration> ttl = ArgumentCaptor.forClass(Duration.class);

        assertEquals(link, service.resolve("short-lived"));

        verify(cache).put(eq("short-lived"), eq(link), ttl.capture());
        assertEquals(Duration.ofMinutes(20), ttl.getValue());
    }

    @Test
    void nonExpiringLinkUsesOneHourPositiveCacheTtl() {
        Link link = link(LinkStatus.ACTIVE, null);
        when(cache.get("durable")).thenReturn(CacheResult.miss());
        when(repository.findByCode("durable")).thenReturn(Optional.of(link));

        service.resolve("durable");

        verify(cache).put("durable", link, Duration.ofHours(1));
    }

    private Link link(LinkStatus status, Instant expiresAt) {
        return new Link(UUID.randomUUID(), "code", "https://example.com", status,
            expiresAt, NOW.minusSeconds(60), NOW.minusSeconds(60));
    }
}
