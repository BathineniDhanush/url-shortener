package com.example.shortener.redirect.application;

import com.example.shortener.link.domain.Link;
import com.example.shortener.link.domain.LinkRepository;
import com.example.shortener.link.domain.LinkStatus;
import com.example.shortener.link.error.LinkNotFoundException;
import com.example.shortener.link.error.LinkUnavailableException;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
public class ResolveLinkService {
    private final LinkRepository linkRepository;
    private final Clock clock;

    public ResolveLinkService(LinkRepository linkRepository, Clock clock) {
        this.linkRepository = linkRepository;
        this.clock = clock;
    }

    public Link resolve(String code) {
        Link link = linkRepository.findByCode(code).orElseThrow(() -> new LinkNotFoundException(code));
        boolean expired = link.expiresAt() != null && !link.expiresAt().isAfter(clock.instant());
        if (link.status() != LinkStatus.ACTIVE || expired) {
            throw new LinkUnavailableException(code);
        }
        return link;
    }
}
