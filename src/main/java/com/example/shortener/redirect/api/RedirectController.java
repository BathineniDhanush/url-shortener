package com.example.shortener.redirect.api;

import com.example.shortener.redirect.application.ResolveLinkService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "api", matchIfMissing = true)
public class RedirectController {
    private final ResolveLinkService resolveLinkService;

    public RedirectController(ResolveLinkService resolveLinkService) {
        this.resolveLinkService = resolveLinkService;
    }

    @GetMapping("/{code:[A-Za-z0-9_-]{4,32}}")
    public ResponseEntity<Void> redirect(@PathVariable String code) {
        var link = resolveLinkService.resolve(code);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(link.destinationUrl()))
                .cacheControl(CacheControl.noStore())
                .header("Referrer-Policy", "no-referrer")
                .build();
    }
}
