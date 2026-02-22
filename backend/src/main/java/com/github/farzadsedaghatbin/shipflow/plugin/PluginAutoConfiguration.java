package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-discovers all Spring beans implementing plugin interfaces
 * and registers them with the {@link PluginRegistry}.
 */
@Configuration
@Slf4j
public class PluginAutoConfiguration {

  @Bean
  CommandLineRunner registerPlugins(
      PluginRegistry registry,
      List<RiskCalculatorPlugin> riskPlugins,
      List<ReportGeneratorPlugin> reportPlugins,
      List<IntegrationProviderPlugin> integrationPlugins) {
    return args -> {
      riskPlugins.forEach(registry::registerRiskPlugin);
      reportPlugins.forEach(registry::registerReportPlugin);
      integrationPlugins.forEach(registry::registerIntegrationPlugin);

      log.info("Plugin registry initialized: {} risk, {} report, {} integration plugins",
          registry.getRiskPlugins().size(),
          registry.getReportPlugins().size(),
          registry.getIntegrationPlugins().size());
    };
  }
}
