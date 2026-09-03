package com.example.shortener;

import com.example.shortener.link.infrastructure.JdbcLinkRepository;
import com.example.shortener.infrastructure.analytics.JdbcAnalyticsRepository;
import com.example.shortener.infrastructure.cache.LinkCache;
import com.example.shortener.redirect.api.AnalyticsPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UrlShortenerApplicationTest {

    @MockitoBean
    private JdbcLinkRepository linkRepository;

    @MockitoBean
    private JdbcAnalyticsRepository analyticsRepository;

    @MockitoBean
    private LinkCache linkCache;

    @MockitoBean
    private AnalyticsPublisher analyticsPublisher;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void applicationStartsAndExposesSystemInformation() throws Exception {
        mockMvc.perform(get("/api/v1/system/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.service").value("url-shortener"))
                .andExpect(jsonPath("$.runtimeRole").value("API"));
    }

    @Test
    void livenessEndpointIsAvailable() throws Exception {
        mockMvc.perform(get("/actuator/health/liveness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
