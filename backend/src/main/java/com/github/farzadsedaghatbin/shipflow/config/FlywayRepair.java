package com.github.farzadsedaghatbin.shipflow.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class FlywayRepair implements CommandLineRunner {

    private final Flyway flyway;

    public FlywayRepair(Flyway flyway) {
        this.flyway = flyway;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("Repairing Flyway schema history...");
        flyway.repair();
        System.out.println("Flyway repair complete.");
    }
}
