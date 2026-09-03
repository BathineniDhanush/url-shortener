package com.example.shortener.redirect.application;

import com.example.shortener.infrastructure.cache.CacheResult;
import com.example.shortener.infrastructure.cache.LinkCache;
import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.error.LinkNotFoundException;
import com.example.shortener.link.error.LinkUnavailableException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

@Service
public class ResolveLinkService {
    private final LinkRepository linkRepository;
    private final Clock clock;
    private final LinkCache linkCache;

    public ResolveLinkService(LinkRepository linkRepository, Clock clock, LinkCache linkCache) {
        this.linkRepository = linkRepository;
        this.clock = clock;
        this.linkCache = linkCache;
    }

    public Link resolve(String code) {
        CacheResult<Link> cacheResult = linkCache.get(code);

        if (cacheResult.isHit()) {
            Link link = cacheResult.getValue();
            boolean expired = link.expiresAt() != null && !link.expiresAt().isAfter(clock.instant());
            if (link.status() == LinkStatus.ACTIVE && !expired) {
                return link;
            }
            // Link is no longer valid, evict from cache and fall through to DB check/exception
            linkCache.evict(code);
        } else if (cacheResult.isNegativeHit()) {
            throw new LinkNotFoundException(code);
        }

        Link link = linkRepository.findByCode(code).orElseGet(() -> {
            linkCache.putNegative(code, Duration.ofMinutes(5));
            throw new LinkNotFoundException(code);
        });

        boolean expired = link.expiresAt() != null && !link.expiresAt().isAfter(clock.instant());
        if (link.status() != LinkStatus.ACTIVE || expired) {
            throw new LinkUnavailableException(code);
        }

        // Cap TTL at remaining lifetime if link expires
        Duration ttl = Duration.ofHours(1);
        if (link.expiresAt() != null) {
            Duration remaining = Duration.between(clock.instant(), link.expiresAt());
            if (remaining.isNegative()) {
                throw new LinkUnavailableException(code);
            }
            ttl = remaining.compareTo(ttl) < 0 ? remaining : ttl;
        }

        linkCache.put(code, link, ttl);
        return link;
    }
}
