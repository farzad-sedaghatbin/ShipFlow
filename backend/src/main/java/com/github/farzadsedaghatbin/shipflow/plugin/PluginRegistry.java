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
  private final List<UiContributionPlugin> uiPlugins = new CopyOnWriteArrayList<>();
  private final Set<String> disabledPluginIds = ConcurrentHashMap.newKeySet();

  // ──────────── Registration ────────────

  public void registerRiskPlugin(RiskCalculatorPlugin plugin) {
    assertIdFree(plugin.getPluginId(), plugin.getClass(), "risk calculator");
    riskPlugins.add(plugin);
    log.info("Registered risk calculator plugin: {} ({})", plugin.getDisplayName(), plugin.getPluginId());
  }

  public void registerReportPlugin(ReportGeneratorPlugin plugin) {
    assertIdFree(plugin.getPluginId(), plugin.getClass(), "report generator");
    reportPlugins.add(plugin);
    log.info("Registered report generator plugin: {} ({})", plugin.getDisplayName(), plugin.getPluginId());
  }

  public void registerIntegrationPlugin(IntegrationProviderPlugin plugin) {
    assertIdFree(plugin.getPluginId(), plugin.getClass(), "integration provider");
    integrationPlugins.add(plugin);
    log.info("Registered integration provider plugin: {} ({})", plugin.getDisplayName(), plugin.getPluginId());
  }

  public void registerUiPlugin(UiContributionPlugin plugin) {
    assertIdFree(plugin.getPluginId(), plugin.getClass(), "UI contribution");
    uiPlugins.add(plugin);
    log.info("Registered UI contribution plugin: {} ({})", plugin.getDisplayName(), plugin.getPluginId());
  }

  /**
   * Plugin ids are the identity for the saved enabled/disabled choice, so two plugins sharing an
   * id (for example an old and a new build of the same plugin both on the classpath) would be
   * indistinguishable. Fail at startup with a message that names both classes.
   */
  private void assertIdFree(String pluginId, Class<?> incoming, String kind) {
    if (pluginId == null || pluginId.isBlank()) {
      throw new IllegalStateException(
          "Plugin " + incoming.getName() + " (" + kind + ") has no plugin id");
    }
    findPluginClass(pluginId)
        .ifPresent(
            existing -> {
              throw new IllegalStateException(
                  "Duplicate plugin id '"
                      + pluginId
                      + "': "
                      + incoming.getName()
                      + " ("
                      + kind
                      + ") clashes with "
                      + existing.getName()
                      + ". Keep only one build of this plugin on the classpath, or give one a"
                      + " different id.");
            });
  }

  private Optional<Class<?>> findPluginClass(String pluginId) {
    return java.util.stream.Stream.<Object>of(
            riskPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).findFirst().orElse(null),
            reportPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).findFirst().orElse(null),
            integrationPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).findFirst().orElse(null),
            uiPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).findFirst().orElse(null))
        .filter(java.util.Objects::nonNull)
        .<Class<?>>map(Object::getClass)
        .findFirst();
  }

  // ──────────── Look-ups ────────────

  public List<UiContributionPlugin> getUiPlugins() {
    return Collections.unmodifiableList(uiPlugins);
  }

  public Optional<UiContributionPlugin> findUiPlugin(String pluginId) {
    return uiPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).findFirst();
  }

  /** Whether a plugin should start enabled the first time it is seen. */
  public boolean defaultEnabled(String pluginId) {
    return findUiPlugin(pluginId).map(UiContributionPlugin::isEnabledByDefault).orElse(true);
  }

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
    riskPlugins.forEach(p -> result.add(toDTO(p.getPluginId(), p.getDisplayName(), "risk", p.getVersion())));
    reportPlugins.forEach(p -> result.add(toDTO(p.getPluginId(), p.getDisplayName(), "report", p.getVersion())));
    integrationPlugins.forEach(p -> result.add(toDTO(p.getPluginId(), p.getDisplayName(), "integration", p.getVersion())));
    uiPlugins.forEach(p -> result.add(toDTO(p.getPluginId(), p.getDisplayName(), "ui", p.getVersion())));
    return Collections.unmodifiableList(result);
  }

  /**
   * Finds a plugin's display name across all type lists. Returns null if not found.
   */
  public String findDisplayName(String pluginId) {
    return riskPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).map(RiskCalculatorPlugin::getDisplayName).findFirst()
        .or(() -> reportPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).map(ReportGeneratorPlugin::getDisplayName).findFirst())
        .or(() -> integrationPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).map(IntegrationProviderPlugin::getDisplayName).findFirst())
        .or(() -> uiPlugins.stream().filter(p -> p.getPluginId().equals(pluginId)).map(UiContributionPlugin::getDisplayName).findFirst())
        .orElse(null);
  }

  /**
   * Returns a summary of all registered plugins.
   */
  public Map<String, Object> getSummary() {
    return Map.of(
        "riskPlugins", riskPlugins.stream().map(RiskCalculatorPlugin::getPluginId).toList(),
        "reportPlugins", reportPlugins.stream().map(ReportGeneratorPlugin::getPluginId).toList(),
        "integrationPlugins", integrationPlugins.stream().map(IntegrationProviderPlugin::getPluginId).toList(),
        "uiPlugins", uiPlugins.stream().map(UiContributionPlugin::getPluginId).toList()
    );
  }

  private PluginDTO toDTO(String pluginId, String displayName, String type, String version) {
    return PluginDTO.builder()
        .pluginId(pluginId)
        .displayName(displayName)
        .type(type)
        .enabled(isEnabled(pluginId))
        .version(cleanVersion(version))
        .build();
  }

  /** A plugin-supplied version is shown to admins, so trim it and cap its length. */
  private static String cleanVersion(String version) {
    if (version == null || version.isBlank()) {
      return null;
    }
    String v = version.strip();
    return v.length() <= 40 ? v : v.substring(0, 40);
  }
}
