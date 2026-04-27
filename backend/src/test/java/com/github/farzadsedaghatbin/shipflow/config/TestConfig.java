package com.github.farzadsedaghatbin.shipflow.config;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Profile;

/** Test configuration to disable AOP aspects in test mode */
@TestConfiguration
@Profile("test")
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class TestConfig {

  // In test mode, aspects are enabled but will not have permission data
  // since Flyway is disabled. Existing tests use @PreAuthorize which works
  // without the RBAC database tables.
}
