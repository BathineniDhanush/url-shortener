package com.example.shortener.analytics.api;

import com.example.shortener.analytics.application.AnalyticsService;
import com.example.shortener.redirect.application.ResolveLinkService;
import com.example.shortener.link.application.ManageLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.Map;

@RestController
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "api", matchIfMissing = true)
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final ManageLinkService manageLinkService;

    public AnalyticsController(AnalyticsService analyticsService, ManageLinkService manageLinkService) {
        this.analyticsService = analyticsService;
        this.manageLinkService = manageLinkService;
    }

    @GetMapping("/api/v1/links/{code}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable String code,
            @RequestHeader("X-Link-Owner-Token") String ownerToken) {
        var link = manageLinkService.get(code, ownerToken).link();
        var totalClicks = analyticsService.getTotalClicks(code, link.id());

        return ResponseEntity.ok(Map.of(
            "code", code,
            "totalClicks", totalClicks
        ));
    }
}
