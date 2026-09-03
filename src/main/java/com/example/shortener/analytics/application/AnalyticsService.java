package com.example.shortener.analytics.application;

import com.example.shortener.domain.analytics.AnalyticsRepository;
import com.example.shortener.domain.analytics.ClickEvent;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class AnalyticsService {
    private final AnalyticsRepository analyticsRepository;

    public AnalyticsService(AnalyticsRepository analyticsRepository) {
        this.analyticsRepository = analyticsRepository;
    }

    public long getTotalClicks(String code, UUID linkId) {
        return analyticsRepository.countByLinkId(linkId);
    }

    public List<ClickEvent> getClickEvents(UUID linkId) {
        return analyticsRepository.findByLinkId(linkId);
    }
}
