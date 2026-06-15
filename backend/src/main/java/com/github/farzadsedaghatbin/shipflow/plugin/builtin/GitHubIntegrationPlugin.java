package com.github.farzadsedaghatbin.shipflow.plugin.builtin;

import com.github.farzadsedaghatbin.shipflow.plugin.IntegrationProviderPlugin;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Built-in integration provider: exposes ShipFlow's GitHub MCP connection as a plugin.
 * Always active — can be disabled via the admin Plugins page.
 */
@Component
public class GitHubIntegrationPlugin implements IntegrationProviderPlugin {

  @Override
  public String getPluginId() {
    return "builtin-github-integration";
  }

  @Override
  public String getDisplayName() {
    return "GitHub Integration";
  }

  @Override
  public List<String> getSupportedActions() {
    return List.of("sync_tasks", "link_pr", "fetch_commits");
  }

  @Override
  public Map<String, Object> execute(String action, Map<String, Object> payload) {
    return Map.of(
        "success", false,
        "message", "Configure the GitHub MCP server in MCP Integration settings to activate this plugin.");
  }
}
