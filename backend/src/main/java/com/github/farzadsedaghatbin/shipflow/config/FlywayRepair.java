package com.github.farzadsedaghatbin.shipflow.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
    name = "spring.flyway.repair.enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class FlywayRepair implements CommandLineRunner {

    private final Flyway flyway;

    @Override
    public void run(String... args) throws Exception {
        log.info("Repairing Flyway schema history...");
        flyway.repair();
        log.info("Flyway repair complete.");
    }
}
