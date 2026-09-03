package com.example.shortener.redirect.application;

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
        Optional<Link> cachedLink = linkCache.get(code);
        if (cachedLink.isPresent()) {
            return cachedLink.get();
        }

        Link link = linkRepository.findByCode(code).orElseGet(() -> {
            linkCache.putNegative(code, Duration.ofMinutes(5));
            throw new LinkNotFoundException(code);
        });

        boolean expired = link.expiresAt() != null && !link.expiresAt().isAfter(clock.instant());
        if (link.status() != LinkStatus.ACTIVE || expired) {
            throw new LinkUnavailableException(code);
        }

        linkCache.put(code, link, Duration.ofHours(1));
        return link;
    }
}
