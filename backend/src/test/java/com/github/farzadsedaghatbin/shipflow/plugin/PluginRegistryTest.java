package com.github.farzadsedaghatbin.shipflow.plugin;

import static org.assertj.core.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PluginRegistryTest {

  private PluginRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new PluginRegistry();
  }

  @Test
  void registeredRiskPlugin_shouldAppearInList() {
    RiskCalculatorPlugin plugin = new SampleRiskCalculatorPlugin();
    registry.registerRiskPlugin(plugin);

    assertThat(registry.getRiskPlugins()).hasSize(1);
    assertThat(registry.getRiskPlugins().get(0).getPluginId()).isEqualTo("sample-risk-heuristic");
  }

  @Test
  void computeAggregatedRisk_shouldAverageScores() {
    // Register two plugins that return fixed scores
    registry.registerRiskPlugin(new RiskCalculatorPlugin() {
      public String getPluginId() { return "p1"; }
      public String getDisplayName() { return "P1"; }
      public Double calculateRisk(Map<String, Object> ctx) { return 0.8; }
    });
    registry.registerRiskPlugin(new RiskCalculatorPlugin() {
      public String getPluginId() { return "p2"; }
      public String getDisplayName() { return "P2"; }
      public Double calculateRisk(Map<String, Object> ctx) { return 0.4; }
    });

    Double aggregated = registry.computeAggregatedRisk(Map.of());

    assertThat(aggregated).isCloseTo(0.6, within(0.001));
  }

  @Test
  void computeAggregatedRisk_shouldReturnNull_whenNoPlugins() {
    Double aggregated = registry.computeAggregatedRisk(Map.of());
    assertThat(aggregated).isNull();
  }

  @Test
  void computeAggregatedRisk_shouldSkipNullResults() {
    registry.registerRiskPlugin(new RiskCalculatorPlugin() {
      public String getPluginId() { return "p1"; }
      public String getDisplayName() { return "P1"; }
      public Double calculateRisk(Map<String, Object> ctx) { return null; }
    });
    registry.registerRiskPlugin(new RiskCalculatorPlugin() {
      public String getPluginId() { return "p2"; }
      public String getDisplayName() { return "P2"; }
      public Double calculateRisk(Map<String, Object> ctx) { return 0.5; }
    });

    Double aggregated = registry.computeAggregatedRisk(Map.of());
    assertThat(aggregated).isCloseTo(0.5, within(0.001));
  }

  @Test
  void summary_shouldListAllPluginIds() {
    registry.registerRiskPlugin(new SampleRiskCalculatorPlugin());

    Map<String, Object> summary = registry.getSummary();

    assertThat(summary).containsKeys("riskPlugins", "reportPlugins", "integrationPlugins");
    @SuppressWarnings("unchecked")
    java.util.List<String> riskPlugins = (java.util.List<String>) summary.get("riskPlugins");
    assertThat(riskPlugins).contains("sample-risk-heuristic");
    @SuppressWarnings("unchecked")
    java.util.List<String> reportPlugins = (java.util.List<String>) summary.get("reportPlugins");
    assertThat(reportPlugins).isEmpty();
  }

  @Test
  void pluginLists_shouldBeUnmodifiable() {
    assertThatThrownBy(() -> registry.getRiskPlugins().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> registry.getReportPlugins().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> registry.getIntegrationPlugins().add(null))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
