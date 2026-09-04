package com.example.shortener;

import com.example.shortener.link.infrastructure.JdbcLinkRepository;
import com.example.shortener.infrastructure.analytics.JdbcAnalyticsRepository;
import com.example.shortener.infrastructure.cache.LinkCache;
import com.example.shortener.redirect.api.AnalyticsPublisher;
import com.example.shortener.observability.ApplicationMetrics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

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

    @Autowired
    private ApplicationMetrics metrics;

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

    @Test
    void openApiContractIsServed() throws Exception {
        mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("openapi: 3.1.0")))
                .andExpect(content().string(containsString("/api/v1/links:")))
                .andExpect(content().string(containsString("/api/v1/links/{code}/analytics:")));
    }

    @Test
    void swaggerUiRendersTheCommittedOpenApiContract() throws Exception {
        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", containsString("/swagger-ui/")));

        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Swagger UI")));
    }

    @Test
    void applicationMetricsAreExposedThroughActuator() throws Exception {
        metrics.cacheLookup(ApplicationMetrics.CacheOutcome.HIT);

        mockMvc.perform(get("/actuator/metrics/url.shortener.cache.lookups"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("url.shortener.cache.lookups"))
                .andExpect(jsonPath("$.availableTags[0].tag").value("result"));
    }
}
