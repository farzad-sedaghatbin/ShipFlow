package com.github.farzadsedaghatbin.shipflow.plugin;

import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginDTO;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Central registry that holds all discovered plugin implementations.
 * Services query the registry to delegate work to registered plugins.
 */
@Component
@Slf4j
public class PluginRegistry {

  private final List<RiskCalculatorPlugin> riskPlugins = new CopyOnWriteArrayList<>();
  private final List<ReportGeneratorPlugin> reportPlugins = new CopyOnWriteArrayList<>();
  private final List<IntegrationProviderPlugin> integrationPlugins = new CopyOnWriteArrayList<>();
  private final Set<String> disabledPluginIds = ConcurrentHashMap.newKeySet();

  // ──────────── Registration ────────────

  public void registerRiskPlugin(RiskCalculatorPlugin plugin) {
    riskPlugins.add(plugin);
    log.info("Registered risk calculator plugin: {} ({})", plugin.getDisplayName(), plugin.getPluginId());
  }

  public void registerReportPlugin(ReportGeneratorPlugin plugin) {
    reportPlugins.add(plugin);
    log.info("Registered report generator plugin: {} ({})", plugin.getDisplayName(), plugin.getPluginId());
  }

  public void registerIntegrationPlugin(IntegrationProviderPlugin plugin) {
    integrationPlugins.add(plugin);
    log.info("Registered integration provider plugin: {} ({})", plugin.getDisplayName(), plugin.getPluginId());
  }

  // ──────────── Look-ups ────────────

  public List<RiskCalculatorPlugin> getRiskPlugins() {
    return Collections.unmodifiableList(riskPlugins);
  }

  public List<ReportGeneratorPlugin> getReportPlugins() {
    return Collections.unmodifiableList(reportPlugins);
  }

  public List<IntegrationProviderPlugin> getIntegrationPlugins() {
    return Collections.unmodifiableList(integrationPlugins);
  }

  public Optional<ReportGeneratorPlugin> findReportPlugin(String pluginId) {
    return reportPlugins.stream()
        .filter(p -> p.getPluginId().equals(pluginId))
        .findFirst();
  }

  public Optional<IntegrationProviderPlugin> findIntegrationPlugin(String pluginId) {
    return integrationPlugins.stream()
        .filter(p -> p.getPluginId().equals(pluginId))
        .findFirst();
  }

  /**
   * Computes an aggregated risk score by averaging all plugin results.
   * Returns null if no plugin produced a score.
   */
  public Double computeAggregatedRisk(Map<String, Object> context) {
    List<Double> scores = riskPlugins.stream()
        .map(p -> {
          try {
            return p.calculateRisk(context);
          } catch (Exception e) {
            log.warn("Risk plugin {} threw an exception: {}", p.getPluginId(), e.getMessage());
            return null;
          }
        })
        .filter(java.util.Objects::nonNull)
        .toList();

    if (scores.isEmpty()) {
      return null;
    }
    return scores.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
  }

  // ──────────── Enable / disable ────────────

  public boolean isEnabled(String pluginId) {
    return !disabledPluginIds.contains(pluginId);
  }

  public void setEnabled(String pluginId, boolean enabled) {
    if (enabled) {
      disabledPluginIds.remove(pluginId);
    } else {
      disabledPluginIds.add(pluginId);
    }
    log.info("Plugin {} {}", pluginId, enabled ? "enabled" : "disabled");
  }

  // ──────────── Admin view ────────────

  public List<PluginDTO> getAllPluginDTOs() {
    List<PluginDTO> result = new ArrayList<>();
    riskPlugins.forEach(p -> result.add(toDTO(p.getPluginId(), p.getDisplayName(), "risk")));
    reportPlugins.forEach(p -> result.add(toDTO(p.getPluginId(), p.getDisplayName(), "report")));
    integrationPlugins.forEach(p -> result.add(toDTO(p.getPluginId(), p.getDisplayName(), "integration")));
    return Collections.unmodifiableList(result);
  }

  /**
   * Finds a plugin's display name across all type lists. Returns null if not found.
   */
  public String findDisplayName(String pluginId) {
    return riskPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).map(RiskCalculatorPlugin::getDisplayName).findFirst()
        .or(() -> reportPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).map(ReportGeneratorPlugin::getDisplayName).findFirst())
        .or(() -> integrationPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).map(IntegrationProviderPlugin::getDisplayName).findFirst())
        .orElse(null);
  }

  /**
   * Returns a summary of all registered plugins.
   */
  public Map<String, Object> getSummary() {
    return Map.of(
        "riskPlugins", riskPlugins.stream().map(RiskCalculatorPlugin::getPluginId).toList(),
        "reportPlugins", reportPlugins.stream().map(ReportGeneratorPlugin::getPluginId).toList(),
        "integrationPlugins", integrationPlugins.stream().map(IntegrationProviderPlugin::getPluginId).toList()
    );
  }

  private PluginDTO toDTO(String pluginId, String displayName, String type) {
    return PluginDTO.builder()
        .pluginId(pluginId)
        .displayName(displayName)
        .type(type)
        .enabled(isEnabled(pluginId))
        .build();
  }
}
