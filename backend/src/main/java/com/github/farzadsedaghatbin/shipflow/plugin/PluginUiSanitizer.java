package com.github.farzadsedaghatbin.shipflow.plugin;

import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginBlockDTO;
import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginMenuItemDTO;
import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginPageDTO;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Validates everything a UI plugin returns before it reaches a browser. Plugin code is trusted to
 * compile into the backend, but its <em>output</em> is still treated as untrusted data: bad ids,
 * unknown icons and unsafe link targets are rejected or dropped, and oversize content is cut to
 * fixed limits instead of failing the whole page.
 */
@Component
public class PluginUiSanitizer {

  /** Keep in sync with {@code frontend/src/config/pluginIcons.ts}. */
  static final Set<String> ALLOWED_ICONS =
      Set.of(
          "puzzle", "chart", "list", "book", "calendar", "shield", "rocket", "flag", "users",
          "settings", "file", "link", "star", "bell");

  static final String DEFAULT_ICON = "puzzle";
  static final int MAX_BLOCKS = 50;
  static final int MAX_STATS = 24;
  static final int MAX_COLUMNS = 20;
  static final int MAX_ROWS = 500;
  static final int MAX_LINKS = 50;
  static final int MAX_MARKDOWN = 100_000;
  static final int MAX_CELL = 1_000;
  static final int MAX_SHORT = 200;
  static final int MAX_LABEL = 60;

  private static final Pattern ID = Pattern.compile("[a-z0-9]+(-[a-z0-9]+)*");
  private static final Pattern LANG = Pattern.compile("[a-z]{2,5}");
  private static final Set<String> ALERT_LEVELS = Set.of("info", "success", "warning", "error");

  public static boolean isValidId(String id) {
    return id != null && id.length() <= 80 && ID.matcher(id).matches();
  }

  /** Returns the cleaned menu item, or {@code null} if it cannot be shown. */
  public PluginMenuItemDTO menuItem(String pluginId, PluginMenuItem item) {
    if (item == null || !isValidId(pluginId) || !isValidId(item.key())) {
      return null;
    }
    Map<String, String> labels = new LinkedHashMap<>();
    item.labels()
        .forEach(
            (lang, text) -> {
              if (lang != null && LANG.matcher(lang).matches() && text != null && !text.isBlank()) {
                labels.put(lang, cut(text.strip(), MAX_LABEL));
              }
            });
    if (!labels.containsKey("en")) {
      return null;
    }
    String icon = item.icon() != null && ALLOWED_ICONS.contains(item.icon()) ? item.icon() : DEFAULT_ICON;
    return PluginMenuItemDTO.builder()
        .pluginId(pluginId)
        .key(item.key())
        .labels(labels)
        .icon(icon)
        .requiredRole(item.requiredRole().name())
        .build();
  }

  public PluginPageDTO page(String pluginId, String pageKey, PluginPage page) {
    List<PluginBlockDTO> blocks = new ArrayList<>();
    for (PluginBlock block : page.blocks()) {
      if (blocks.size() >= MAX_BLOCKS) {
        break;
      }
      PluginBlockDTO clean = block(block);
      if (clean != null) {
        blocks.add(clean);
      }
    }
    String title = page.title() == null || page.title().isBlank() ? pageKey : page.title().strip();
    return PluginPageDTO.builder()
        .pluginId(pluginId)
        .pageKey(pageKey)
        .title(cut(title, MAX_SHORT))
        .blocks(blocks)
        .build();
  }

  private PluginBlockDTO block(PluginBlock block) {
    if (block == null || block.type() == null) {
      return null;
    }
    Map<String, Object> data = new LinkedHashMap<>();
    Map<String, Object> in = block.data();
    switch (block.type()) {
      case MARKDOWN -> data.put("text", cut(str(in.get("text")), MAX_MARKDOWN));
      case ALERT -> {
        String level = str(in.get("level"));
        data.put("level", ALERT_LEVELS.contains(level) ? level : "info");
        data.put("text", cut(str(in.get("text")), 2_000));
      }
      case STAT_CARDS -> data.put("stats", stats(in.get("stats")));
      case TABLE -> {
        List<String> columns = new ArrayList<>();
        for (Object c : list(in.get("columns"))) {
          if (columns.size() < MAX_COLUMNS) {
            columns.add(cut(str(c), MAX_SHORT));
          }
        }
        List<List<String>> rows = new ArrayList<>();
        for (Object r : list(in.get("rows"))) {
          if (rows.size() >= MAX_ROWS) {
            break;
          }
          List<String> row = new ArrayList<>();
          for (Object cell : list(r)) {
            if (row.size() < columns.size()) {
              row.add(cut(str(cell), MAX_CELL));
            }
          }
          while (row.size() < columns.size()) {
            row.add("");
          }
          rows.add(row);
        }
        data.put("columns", columns);
        data.put("rows", rows);
      }
      case LINK_LIST -> data.put("links", links(in.get("links")));
      default -> {
        return null;
      }
    }
    return PluginBlockDTO.builder().type(block.type().name()).data(data).build();
  }

  private List<Map<String, Object>> stats(Object raw) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : list(raw)) {
      if (out.size() >= MAX_STATS || !(o instanceof Map<?, ?> m)) {
        continue;
      }
      Map<String, Object> stat = new LinkedHashMap<>();
      stat.put("label", cut(str(m.get("label")), MAX_SHORT));
      stat.put("value", cut(str(m.get("value")), MAX_SHORT));
      if (m.get("hint") != null) {
        stat.put("hint", cut(str(m.get("hint")), MAX_SHORT));
      }
      out.add(stat);
    }
    return out;
  }

  private List<Map<String, Object>> links(Object raw) {
    List<Map<String, Object>> out = new ArrayList<>();
    for (Object o : list(raw)) {
      if (out.size() >= MAX_LINKS || !(o instanceof Map<?, ?> m)) {
        continue;
      }
      String target = str(m.get("target")).strip();
      if (!isSafeTarget(target)) {
        continue;
      }
      Map<String, Object> link = new LinkedHashMap<>();
      link.put("label", cut(str(m.get("label")), MAX_SHORT));
      link.put("target", target);
      out.add(link);
    }
    return out;
  }

  /** An in-app path ({@code /x}, not {@code //host}) or an {@code https://} URL with a host. */
  static boolean isSafeTarget(String target) {
    if (target == null || target.isEmpty() || target.length() > 2_000) {
      return false;
    }
    for (char c : target.toCharArray()) {
      if (c < 0x20 || c == 0x7f || c == '\\') {
        return false;
      }
    }
    if (target.startsWith("/")) {
      return !target.startsWith("//");
    }
    if (target.startsWith("https://")) {
      try {
        return URI.create(target).getHost() != null;
      } catch (IllegalArgumentException e) {
        return false;
      }
    }
    return false;
  }

  private static List<?> list(Object o) {
    return o instanceof List<?> l ? l : List.of();
  }

  private static String str(Object o) {
    return o == null ? "" : o.toString();
  }

  private static String cut(String s, int max) {
    return s.length() <= max ? s : s.substring(0, max);
  }
}
