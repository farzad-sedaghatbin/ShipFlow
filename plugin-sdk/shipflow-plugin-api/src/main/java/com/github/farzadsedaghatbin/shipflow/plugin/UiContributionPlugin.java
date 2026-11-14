package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.List;

/**
 * SPI for plugins that add entries to ShipFlow's sidebar and the pages they open.
 *
 * <p>Pages are declarative: {@link #getPage} returns {@link PluginBlock}s (markdown, stat cards,
 * tables, links, alerts) and ShipFlow renders them. A plugin cannot ship its own JavaScript or
 * HTML. Role requirements are enforced by the backend.
 *
 * <p>Available since SDK 1.1.0. This is a separate interface (not new methods on the older three)
 * so plugins compiled against 1.0.0 keep working.
 */
public interface UiContributionPlugin {

  /** Stable unique id, lowercase letters, digits and hyphens only (used in the page URL). */
  String getPluginId();

  /** Name shown in Organization Settings → Plugins. */
  String getDisplayName();

  /**
   * The plugin's own version (for example {@code 1.2.0}), shown in Organization Settings →
   * Plugins so an administrator can tell which build is running. Optional: {@code null} means
   * "not stated". Added in SDK 1.1.0 as a default method, so older plugins still compile and run.
   */
  default String getVersion() {
    return null;
  }

  /** Sidebar entries this plugin adds. Called each time the menu is built, so keep it cheap. */
  List<PluginMenuItem> getMenuItems();

  /**
   * Builds the page for one of this plugin's menu keys.
   *
   * @param pageKey a key returned by {@link #getMenuItems}
   * @param context who is asking
   * @return the page, or {@code null} if the key is unknown (reported as "not found")
   */
  PluginPage getPage(String pageKey, PluginPageContext context);

  /**
   * Whether the plugin starts enabled the first time ShipFlow sees it. After that the
   * administrator's saved choice wins.
   */
  default boolean isEnabledByDefault() {
    return true;
  }
}
