package com.github.farzadsedaghatbin.shipflow.plugin;

import java.util.List;
import java.util.Map;

/**
 * A unit of plugin page content. Build one with the static factories; the {@code data} map is the
 * wire format the frontend renders, so new block types can be added without breaking old plugins.
 *
 * <p>ShipFlow validates every block before sending it to a browser (size limits, link targets).
 */
public record PluginBlock(Type type, Map<String, Object> data) {

  public enum Type {
    MARKDOWN,
    STAT_CARDS,
    TABLE,
    LINK_LIST,
    ALERT
  }

  /** A headline number. {@code hint} may be {@code null}. */
  public record Stat(String label, String value, String hint) {}

  /**
   * A link. {@code target} must be an in-app path starting with {@code /} or an {@code https://}
   * URL; anything else is dropped.
   */
  public record Link(String label, String target) {}

  public PluginBlock {
    data = data == null ? Map.of() : Map.copyOf(data);
  }

  /** Markdown text (sanitised when rendered, so raw HTML does not run). */
  public static PluginBlock markdown(String text) {
    return new PluginBlock(Type.MARKDOWN, Map.of("text", text == null ? "" : text));
  }

  public static PluginBlock statCards(List<Stat> stats) {
    return new PluginBlock(
        Type.STAT_CARDS,
        Map.of(
            "stats",
            stats.stream()
                .map(
                    s -> {
                      Map<String, Object> m = new java.util.LinkedHashMap<>();
                      m.put("label", s.label());
                      m.put("value", s.value());
                      if (s.hint() != null) {
                        m.put("hint", s.hint());
                      }
                      return m;
                    })
                .toList()));
  }

  public static PluginBlock table(List<String> columns, List<List<String>> rows) {
    return new PluginBlock(Type.TABLE, Map.of("columns", columns, "rows", rows));
  }

  public static PluginBlock linkList(List<Link> links) {
    return new PluginBlock(
        Type.LINK_LIST,
        Map.of(
            "links",
            links.stream()
                .map(l -> Map.<String, Object>of("label", l.label(), "target", l.target()))
                .toList()));
  }

  /** {@code level} is one of {@code info}, {@code success}, {@code warning}, {@code error}. */
  public static PluginBlock alert(String level, String text) {
    return new PluginBlock(
        Type.ALERT, Map.of("level", level == null ? "info" : level, "text", text == null ? "" : text));
  }
}
