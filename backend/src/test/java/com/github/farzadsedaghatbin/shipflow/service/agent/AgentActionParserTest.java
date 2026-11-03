package com.github.farzadsedaghatbin.shipflow.service.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentActionParser")
class AgentActionParserTest {

  private final AgentActionParser parser = new AgentActionParser(new ObjectMapper());

  @Test
  @DisplayName("Reads the actions and strips the block from the visible text")
  void parsesBlock() {
    var parsed =
        parser.parse(
            """
            It is blocked on review. I can mark it done.

            ```shipflow-actions
            [{"tool":"update_task_status","arguments":{"taskId":3,"status":"DONE"},"summary":"Mark done"}]
            ```
            """);

    assertThat(parsed.visibleText()).isEqualTo("It is blocked on review. I can mark it done.");
    assertThat(parsed.actions()).hasSize(1);
    assertThat(parsed.actions().get(0).tool()).isEqualTo("update_task_status");
    assertThat(parsed.actions().get(0).arguments()).containsEntry("status", "DONE").containsKey("taskId");
    assertThat(parsed.actions().get(0).summary()).isEqualTo("Mark done");
  }

  @Test
  @DisplayName("Text with no block is returned unchanged with no actions")
  void noBlock() {
    var parsed = parser.parse("Just an answer, with ```code``` in it.");
    assertThat(parsed.visibleText()).isEqualTo("Just an answer, with ```code``` in it.");
    assertThat(parsed.actions()).isEmpty();
  }

  @Test
  @DisplayName("A malformed block yields no actions but is still hidden from the reader")
  void malformedBlock() {
    var parsed = parser.parse("Answer.\n```shipflow-actions\n[{not json\n```");
    assertThat(parsed.visibleText()).isEqualTo("Answer.");
    assertThat(parsed.actions()).isEmpty();
  }

  @Test
  @DisplayName("An unterminated block (model ran out of tokens) is hidden and parsed if it is whole")
  void unterminated() {
    var parsed =
        parser.parse(
            "Answer.\n```shipflow-actions\n[{\"tool\":\"update_task_status\",\"arguments\":{\"taskId\":1,\"status\":\"DONE\"}}]");
    assertThat(parsed.visibleText()).isEqualTo("Answer.");
    assertThat(parsed.actions()).hasSize(1);
  }

  @Test
  @DisplayName("Items missing a tool or object arguments are skipped; a lone object is accepted")
  void skipsBadItems() {
    var parsed =
        parser.parse(
            "```shipflow-actions\n[{\"arguments\":{}},{\"tool\":\"x\",\"arguments\":\"no\"},{\"tool\":\"update_task\",\"arguments\":{\"taskId\":2}}]\n```");
    assertThat(parsed.actions()).extracting(a -> a.tool()).containsExactly("update_task");

    var single = parser.parse("```shipflow-actions\n{\"tool\":\"update_task\",\"arguments\":{\"taskId\":2}}\n```");
    assertThat(single.actions()).hasSize(1);
  }

  @Test
  @DisplayName("Handles null and the fence label in any case")
  void edgeCases() {
    assertThat(parser.parse(null).visibleText()).isEmpty();
    var upper =
        parser.parse("A\n```SHIPFLOW-ACTIONS\n[{\"tool\":\"update_task\",\"arguments\":{\"taskId\":2}}]\n```");
    assertThat(upper.actions()).hasSize(1);
  }

  @Test
  @DisplayName("An over-long summary is clipped")
  void clipsSummary() {
    String longSummary = "s".repeat(1000);
    var parsed =
        parser.parse(
            "```shipflow-actions\n[{\"tool\":\"update_task\",\"arguments\":{\"taskId\":2},\"summary\":\""
                + longSummary + "\"}]\n```");
    assertThat(parsed.actions().get(0).summary().length()).isLessThan(400);
  }
}
