package com.example.plugin;

import com.github.farzadsedaghatbin.shipflow.plugin.PluginBlock;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginMenuItem;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginPage;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginPageContext;
import com.github.farzadsedaghatbin.shipflow.plugin.UiContributionPlugin;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Example ShipFlow UI plugin: adds a "Status" entry to the sidebar and a page built from data
 * blocks. A page is data, not code — ShipFlow renders the blocks, so a plugin never runs
 * JavaScript in the browser. Requires shipflow-plugin-api 1.1.0 or later.
 */
@Component
public class SampleStatusPagePlugin implements UiContributionPlugin {

  private static final String PAGE_KEY = "status";

  @Override
  public String getPluginId() {
    return "sample-status-page";
  }

  @Override
  public String getDisplayName() {
    return "Sample Status Page";
  }

  @Override
  public String getVersion() {
    return "1.0.0"; // bump this each time you ship a new build of your plugin
  }

  @Override
  public List<PluginMenuItem> getMenuItems() {
    return List.of(
        new PluginMenuItem(
            PAGE_KEY, Map.of("en", "Status"), "chart", PluginMenuItem.RequiredRole.ANY));
  }

  @Override
  public PluginPage getPage(String pageKey, PluginPageContext context) {
    if (!PAGE_KEY.equals(pageKey)) {
      return null; // reported to the user as "not found"
    }
    return new PluginPage(
        "Status",
        List.of(
            PluginBlock.markdown("Hello **" + context.username() + "**."),
            PluginBlock.statCards(List.of(new PluginBlock.Stat("Your role", context.role(), null))),
            PluginBlock.linkList(List.of(new PluginBlock.Link("Open the backlog", "/backlog")))));
  }
}
