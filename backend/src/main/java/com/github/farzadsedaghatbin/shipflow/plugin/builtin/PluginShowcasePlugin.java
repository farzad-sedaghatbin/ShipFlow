package com.github.farzadsedaghatbin.shipflow.plugin.builtin;

import com.github.farzadsedaghatbin.shipflow.plugin.PluginBlock;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginMenuItem;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginPage;
import com.github.farzadsedaghatbin.shipflow.plugin.PluginPageContext;
import com.github.farzadsedaghatbin.shipflow.plugin.UiContributionPlugin;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Reference UI plugin: one sidebar entry and one page that uses every block type. It starts
 * <b>disabled</b>, so ordinary installs get no extra menu entry; an administrator can switch it
 * on under Organization Settings → Plugins to see what a plugin page looks like.
 */
@Component
public class PluginShowcasePlugin implements UiContributionPlugin {

  public static final String PLUGIN_ID = "builtin-plugin-showcase";
  public static final String PAGE_KEY = "overview";

  @Override
  public String getPluginId() {
    return PLUGIN_ID;
  }

  @Override
  public String getDisplayName() {
    return "Plugin Showcase";
  }

  @Override
  public String getVersion() {
    return "1.0.0";
  }

  @Override
  public boolean isEnabledByDefault() {
    return false;
  }

  @Override
  public List<PluginMenuItem> getMenuItems() {
    return List.of(
        new PluginMenuItem(
            PAGE_KEY,
            Map.of("en", "Plugin Showcase", "fa", "نمایش افزونه"),
            "puzzle",
            PluginMenuItem.RequiredRole.ANY));
  }

  @Override
  public PluginPage getPage(String pageKey, PluginPageContext context) {
    if (!PAGE_KEY.equals(pageKey)) {
      return null;
    }
    return new PluginPage(
        "Plugin Showcase",
        List.of(
            PluginBlock.markdown(
                "## Hello, " + context.username() + "\n\nThis page is built by a **plugin** and "
                    + "rendered by ShipFlow. A plugin returns data blocks; it cannot run its own "
                    + "JavaScript.\n\n- Markdown\n- Stat cards\n- Tables\n- Links\n- Alerts"),
            PluginBlock.statCards(
                List.of(
                    new PluginBlock.Stat("Your role", context.role(), null),
                    new PluginBlock.Stat("Language", context.locale(), "from your profile"),
                    new PluginBlock.Stat(
                        "Project", context.projectId() == null ? "none" : "#" + context.projectId(), null))),
            PluginBlock.table(
                List.of("Block type", "Use it for"),
                List.of(
                    List.of("MARKDOWN", "Explanations and formatted text"),
                    List.of("STAT_CARDS", "Headline numbers"),
                    List.of("TABLE", "Lists of records"),
                    List.of("LINK_LIST", "Jumps to ShipFlow pages or https links"),
                    List.of("ALERT", "Notices and warnings"))),
            PluginBlock.linkList(
                List.of(
                    new PluginBlock.Link("Open the dashboard", "/dashboard"),
                    new PluginBlock.Link("Open the backlog", "/backlog"))),
            PluginBlock.alert("info", "This plugin is disabled by default. Turn it on under Organization Settings → Plugins.")));
  }
}
