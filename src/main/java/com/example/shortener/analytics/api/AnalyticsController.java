package com.example.shortener.analytics.api;

import com.example.shortener.analytics.application.AnalyticsService;
import com.example.shortener.redirect.application.ResolveLinkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AnalyticsController {
    private final AnalyticsService analyticsService;
    private final ResolveLinkService resolveLinkService;

    public AnalyticsController(AnalyticsService analyticsService, ResolveLinkService resolveLinkService) {
        this.analyticsService = analyticsService;
        this.resolveLinkService = resolveLinkService;
    }

    @GetMapping("/api/v1/links/{code}/analytics")
    public ResponseEntity<Map<String, Object>> getAnalytics(@PathVariable String code) {
        var link = resolveLinkService.resolve(code);
        var totalClicks = analyticsService.getTotalClicks(code, link.id());

        return ResponseEntity.ok(Map.of(
            "code", code,
            "totalClicks", totalClicks
        ));
    }
}
