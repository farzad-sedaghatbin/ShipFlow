package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.Map;

/**
 * One entry a {@link UiContributionPlugin} adds to ShipFlow's sidebar.
 *
 * <p>The entry opens the plugin page whose key equals {@link #key()}.
 *
 * @param key page key passed back to {@link UiContributionPlugin#getPage}; lowercase letters,
 *     digits and hyphens only
 * @param labels display text by language code ({@code "en"} is required, {@code "fa"} optional);
 *     the app falls back to {@code "en"} when the user's language is missing
 * @param icon name from the icon allow-list (for example {@code puzzle}, {@code chart}, {@code
 *     list}, {@code book}); unknown names fall back to a generic icon
 * @param requiredRole the lowest role that sees the entry and may open the page
 */
public record PluginMenuItem(
    String key, Map<String, String> labels, String icon, RequiredRole requiredRole) {

  /** Lowest role allowed to see a menu entry. Enforced by the backend, not only hidden in the UI. */
  public enum RequiredRole {
    ANY,
    MANAGER,
    ADMIN
  }

  public PluginMenuItem {
    labels = labels == null ? Map.of() : Map.copyOf(labels);
    requiredRole = requiredRole == null ? RequiredRole.ANY : requiredRole;
  }

  /** Convenience factory for an English-only entry visible to every signed-in user. */
  public static PluginMenuItem of(String key, String englishLabel, String icon) {
    return new PluginMenuItem(key, Map.of("en", englishLabel), icon, RequiredRole.ANY);
  }
}
