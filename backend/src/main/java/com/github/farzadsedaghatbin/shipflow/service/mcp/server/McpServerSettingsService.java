package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import com.github.farzadsedaghatbin.shipflow.config.mcp.McpServerProperties;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the <em>effective</em> MCP server enablement state.
 *
 * <p>The MCP server can be controlled two ways:
 *
 * <ul>
 *   <li><b>Environment variables</b> — {@code MCP_SERVER_ENABLED} / {@code MCP_SERVER_WRITE_ENABLED}
 *       (handled by {@link McpServerProperties}). These act as the default.
 *   <li><b>Runtime admin toggle</b> — stored on {@link OrganizationSettings}. When an admin sets a
 *       value from the UI it overrides the environment default, taking effect without a restart.
 * </ul>
 *
 * <p>Resolution rule: if the DB override is non-null, use it; otherwise fall back to the
 * environment-variable default. This keeps existing self-hosted deployments behaving exactly as
 * before until an admin explicitly flips the toggle.
 *
 * <p>This bean always exists (it is not gated by {@code @ConditionalOnProperty}) so that the request
 * pipeline can decide, per-request, whether the MCP server should respond.
 */
@Service
@RequiredArgsConstructor
public class McpServerSettingsService {

  private final OrganizationSettingsRepository settingsRepository;
  private final McpServerProperties properties;

  /** Whether the MCP server is currently enabled (DB override, else env default). */
  @Transactional(readOnly = true)
  public boolean isEnabled() {
    return resolveEnabled(loadSettings());
  }

  /**
   * Whether MCP write tools are currently available. Write tools require BOTH the server and write
   * mode to be enabled.
   */
  @Transactional(readOnly = true)
  public boolean isWriteEnabled() {
    OrganizationSettings settings = loadSettings();
    if (!resolveEnabled(settings)) {
      return false;
    }
    return resolveWriteEnabled(settings);
  }

  /** Loads the singleton settings row, or {@code null} when none exists yet. */
  private OrganizationSettings loadSettings() {
    return settingsRepository.findFirstByOrderByIdAsc().orElse(null);
  }

  private boolean resolveEnabled(OrganizationSettings settings) {
    if (settings != null && settings.getMcpServerEnabled() != null) {
      return settings.getMcpServerEnabled();
    }
    return properties.isEnabled();
  }

  private boolean resolveWriteEnabled(OrganizationSettings settings) {
    if (settings != null && settings.getMcpServerWriteEnabled() != null) {
      return settings.getMcpServerWriteEnabled();
    }
    return properties.isWriteEnabled();
  }
}
