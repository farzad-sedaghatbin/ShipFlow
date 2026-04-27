package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.Map;

/**
 * Extension point for external tool integrations.
 * Implementations can send data to or receive data from external services.
 */
public interface IntegrationProviderPlugin {

  /** Unique identifier for this plugin. */
  String getPluginId();

  /** Human-readable name shown in settings. */
  String getDisplayName();

  /**
   * Execute an integration action.
   *
   * @param action  the action name, e.g. "sync_tasks", "notify", "import"
   * @param payload the action-specific data
   * @return a result map (can include "success", "message", etc.)
   */
  Map<String, Object> execute(String action, Map<String, Object> payload);

  /**
   * Returns a list of actions supported by this plugin.
   */
  java.util.List<String> getSupportedActions();
}
