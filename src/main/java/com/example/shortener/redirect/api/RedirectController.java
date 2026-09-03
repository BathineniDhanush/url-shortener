package com.example.shortener.redirect.api;

import com.example.shortener.redirect.application.ResolveLinkService;
import com.example.shortener.domain.analytics.ClickEvent;
import com.example.shortener.domain.util.PrivacyUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "api", matchIfMissing = true)
public class RedirectController {
    private final ResolveLinkService resolveLinkService;
    private final AnalyticsPublisher analyticsPublisher;

    public RedirectController(ResolveLinkService resolveLinkService, AnalyticsPublisher analyticsPublisher) {
        this.resolveLinkService = resolveLinkService;
        this.analyticsPublisher = analyticsPublisher;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]{4,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String code, HttpServletRequest request) {
        var link = resolveLinkService.resolve(code);

        var event = new ClickEvent(
            UUID.randomUUID(),
            link.id(),
            Instant.now(),
            PrivacyUtils.anonymizeIp(request.getRemoteAddr()),
            PrivacyUtils.filterUserAgent(request.getHeader("User-Agent"))
        );
        analyticsPublisher.publish(event);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.destinationUrl()))
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .build();
    }
}
