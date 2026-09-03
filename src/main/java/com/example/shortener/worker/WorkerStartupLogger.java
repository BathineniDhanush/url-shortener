package com.example.shortener.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.runtime.role", havingValue = "worker")
public class WorkerStartupLogger implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(WorkerStartupLogger.class);

    @Override
    public void run(ApplicationArguments args) {
        LOGGER.info("URL shortener worker runtime started");
    }
}
