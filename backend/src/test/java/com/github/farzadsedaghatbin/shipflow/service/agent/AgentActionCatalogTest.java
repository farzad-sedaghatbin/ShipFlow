package com.github.farzadsedaghatbin.shipflow.service.agent;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentActionCatalog")
class AgentActionCatalogTest {

  private final AgentActionCatalog catalog = new AgentActionCatalog();

  @Test
  @DisplayName("Allows only the task, bug and pitch tools — nothing else")
  void allowList() {
    assertThat(catalog.toolNames())
        .containsExactlyInAnyOrder(
            "create_task", "update_task", "update_task_status", "update_task_assignee",
            "create_bug", "update_bug_status", "update_bug_assignee",
            "update_pitch", "update_pitch_status");
    // Write tools the agent must never propose:
    for (String forbidden : new String[] {"wise_architecture_analyze", "log_work", "create_initiative",
        "create_epic", "update_epic", "add_epic_dependency", "link_task_to_epic", "add_comment",
        "record_test_run", "create_pitch", "get_task"}) {
      assertThat(catalog.isAllowed(forbidden)).as(forbidden).isFalse();
    }
    assertThat(catalog.isAllowed(null)).isFalse();
  }

  @Test
  @DisplayName("The prompt lists each tool with its parameters, required ones and enum values")
  void promptSection() {
    String prompt = catalog.promptSection();
    assertThat(prompt).contains("- update_task_status:");
    assertThat(prompt).contains("taskId (required integer)");
    assertThat(prompt).contains("status (required string: TODO|IN_PROGRESS|IN_REVIEW|DONE|BLOCKED)");
    assertThat(prompt).doesNotContain("Requires WRITE");
  }

  @Test
  @DisplayName("describe() shows the real tool and arguments, sorted, with long values clipped")
  void describe() {
    Map<String, Object> args = new LinkedHashMap<>();
    args.put("status", "DONE");
    args.put("taskId", 3);
    args.put("note", "x".repeat(200));

    String label = catalog.describe("update_task_status", args);

    assertThat(label).startsWith("update_task_status(note=xxx");
    assertThat(label).contains("status=DONE, taskId=3)");
    assertThat(label.length()).isLessThan(200);
    assertThat(catalog.describe("update_task", null)).isEqualTo("update_task()");
  }
}
