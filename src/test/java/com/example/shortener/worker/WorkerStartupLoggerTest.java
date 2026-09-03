package com.example.shortener.worker;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class WorkerStartupLoggerTest {
    @Test
    void logsStartupWithoutRequiringArguments() {
        assertThatCode(() -> new WorkerStartupLogger().run(null)).doesNotThrowAnyException();
    }
}
