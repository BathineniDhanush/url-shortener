package com.example.shortener.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkerConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(new ConfigDataApplicationContextInitializer())
            .withPropertyValues("spring.profiles.active=worker");

    @Test
    void redisCommandTimeoutExceedsAnalyticsBlockingReadTimeout() {
        contextRunner.run(context -> {
            Duration commandTimeout = Binder.get(context.getEnvironment())
                    .bind("spring.data.redis.timeout", Duration.class)
                    .orElseThrow(() -> new AssertionError("Worker Redis command timeout is not configured"));

            assertTrue(commandTimeout.compareTo(Duration.ofSeconds(5)) > 0,
                    "Worker Redis command timeout must exceed the five-second blocking read timeout");
        });
    }
}
