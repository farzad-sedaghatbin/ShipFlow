package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("OutboundText")
class OutboundTextTest {

  @Test
  @DisplayName("Broadcast mentions in every spelling are removed")
  void broadcastMentions() {
    assertThat(OutboundText.clean("hey <!channel> and <!here|here> and @everyone and @Channel, ok"))
        .doesNotContain("channel").doesNotContain("here").doesNotContain("everyone")
        .contains("hey").contains("ok");
  }

  @Test
  @DisplayName("Slack link/user syntax can't become a live link or ping")
  void angleBrackets() {
    String cleaned = OutboundText.clean("fix <https://evil.example|click here> and <@U123>");
    assertThat(cleaned).doesNotContain("<").doesNotContain(">");
    assertThat(cleaned).contains("evil.example"); // visible as text, not as a disguised link
  }

  @Test
  @DisplayName("Ordinary names and @-less words are untouched; an email-like @ is not a broadcast")
  void leavesOrdinaryTextAlone() {
    assertThat(OutboundText.clean("Fix login — Sara Jahani")).isEqualTo("Fix login — Sara Jahani");
    assertThat(OutboundText.clean("here is the channelling plan")).isEqualTo("here is the channelling plan");
  }

  @Test
  @DisplayName("line() is single-line and length-capped")
  void line() {
    assertThat(OutboundText.line("a\nb\r\nc", 50)).isEqualTo("a b c");
    String capped = OutboundText.line("x".repeat(200), 20);
    assertThat(capped).hasSize(20).endsWith("…");
    assertThat(OutboundText.line(null, 10)).isEmpty();
  }
}
