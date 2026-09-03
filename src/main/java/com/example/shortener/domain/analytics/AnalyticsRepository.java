package com.example.shortener.domain.analytics;

import java.util.List;
import java.util.UUID;

public interface AnalyticsRepository {
    void save(ClickEvent event);
    List<ClickEvent> findByLinkId(UUID linkId);
    long countByLinkId(UUID linkId);
}
