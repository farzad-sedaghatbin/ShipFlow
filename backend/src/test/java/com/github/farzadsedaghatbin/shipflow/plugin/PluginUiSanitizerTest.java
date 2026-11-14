package com.github.farzadsedaghatbin.shipflow.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginBlockDTO;
import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginMenuItemDTO;
import com.github.farzadsedaghatbin.shipflow.dto.plugin.PluginPageDTO;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PluginUiSanitizerTest {

  private final PluginUiSanitizer sanitizer = new PluginUiSanitizer();

  @Test
  void idsMustBeLowercaseKebab() {
    assertThat(PluginUiSanitizer.isValidId("my-plugin-2")).isTrue();
    assertThat(PluginUiSanitizer.isValidId("My_Plugin")).isFalse();
    assertThat(PluginUiSanitizer.isValidId("../etc")).isFalse();
    assertThat(PluginUiSanitizer.isValidId("a--b")).isFalse();
    assertThat(PluginUiSanitizer.isValidId(null)).isFalse();
  }

  @Test
  void unsafeLinkTargetsAreDropped() {
    PluginPage page =
        new PluginPage(
            "t",
            List.of(
                PluginBlock.linkList(
                    List.of(
                        new PluginBlock.Link("ok", "/backlog"),
                        new PluginBlock.Link("ext", "https://example.com/x"),
                        new PluginBlock.Link("js", "javascript:alert(1)"),
                        new PluginBlock.Link("data", "data:text/html,<script>"),
                        new PluginBlock.Link("proto-relative", "//evil.example"),
                        new PluginBlock.Link("http", "http://example.com"),
                        new PluginBlock.Link("backslash", "/\\evil.example")))));

    PluginPageDTO dto = sanitizer.page("p", "k", page);

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> links =
        (List<Map<String, Object>>) dto.getBlocks().get(0).getData().get("links");
    assertThat(links).extracting(l -> l.get("target")).containsExactly("/backlog", "https://example.com/x");
  }

  @Test
  void menuItemNeedsEnglishLabelAndValidKey() {
    assertThat(sanitizer.menuItem("p", new PluginMenuItem("k", Map.of("fa", "x"), "puzzle", null))).isNull();
    assertThat(sanitizer.menuItem("p", PluginMenuItem.of("Bad Key", "x", "puzzle"))).isNull();
    assertThat(sanitizer.menuItem("BAD", PluginMenuItem.of("k", "x", "puzzle"))).isNull();
  }

  @Test
  void unknownIconFallsBackAndLabelIsCut() {
    PluginMenuItemDTO dto =
        sanitizer.menuItem("p", PluginMenuItem.of("k", "x".repeat(500), "<script>"));

    assertThat(dto.getIcon()).isEqualTo("puzzle");
    assertThat(dto.getLabels().get("en")).hasSize(PluginUiSanitizer.MAX_LABEL);
    assertThat(dto.getRequiredRole()).isEqualTo("ANY");
  }

  @Test
  void oversizeContentIsCutNotRejected() {
    List<PluginBlock> blocks = new ArrayList<>();
    for (int i = 0; i < 80; i++) {
      blocks.add(PluginBlock.markdown("x"));
    }
    blocks.set(0, PluginBlock.markdown("m".repeat(PluginUiSanitizer.MAX_MARKDOWN + 10)));
    List<List<String>> rows = new ArrayList<>();
    for (int i = 0; i < 600; i++) {
      rows.add(List.of("a", "b", "extra"));
    }
    blocks.set(1, PluginBlock.table(List.of("c1", "c2"), rows));

    PluginPageDTO dto = sanitizer.page("p", "k", new PluginPage("t", blocks));

    assertThat(dto.getBlocks()).hasSize(PluginUiSanitizer.MAX_BLOCKS);
    assertThat((String) dto.getBlocks().get(0).getData().get("text")).hasSize(PluginUiSanitizer.MAX_MARKDOWN);
    PluginBlockDTO table = dto.getBlocks().get(1);
    assertThat((List<?>) table.getData().get("rows")).hasSize(PluginUiSanitizer.MAX_ROWS);
    assertThat((List<?>) ((List<?>) table.getData().get("rows")).get(0)).hasSize(2);
  }

  @Test
  void unknownAlertLevelBecomesInfo() {
    PluginPageDTO dto = sanitizer.page("p", "k", new PluginPage(null, List.of(PluginBlock.alert("fatal", "x"))));

    assertThat(dto.getBlocks().get(0).getData().get("level")).isEqualTo("info");
    assertThat(dto.getTitle()).isEqualTo("k");
  }
}
