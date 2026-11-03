package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.service.agent.loop.AgentLoopConfig.Channel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AgentLoopConfig")
class AgentLoopConfigTest {

  private final ObjectMapper mapper = new ObjectMapper();

  private AgentLoopConfig parse(String json) {
    return AgentLoopConfig.parse(json, mapper);
  }

  @Test
  @DisplayName("Empty settings take safe defaults: in-app only, 24 h, AI on, quiet when healthy")
  void defaults() {
    for (String json : new String[] {null, "", "{}"}) {
      AgentLoopConfig c = parse(json);
      assertThat(c.channels()).containsExactly(Channel.IN_APP);
      assertThat(c.channel()).isNull();
      assertThat(c.lookbackHours()).isEqualTo(24);
      assertThat(c.useAi()).isTrue();
      assertThat(c.onlyWhenAtRisk()).isTrue();
    }
  }

  @Test
  @DisplayName("A full config is read as written")
  void full() {
    AgentLoopConfig c = parse(
        "{\"channels\":[\"slack\",\"TEAMS\",\"IN_APP\"],\"channel\":\"#standup\",\"lookbackHours\":48,"
            + "\"onlyWhenAtRisk\":false,\"useAi\":false}");

    assertThat(c.channels()).containsExactlyInAnyOrder(Channel.SLACK, Channel.TEAMS, Channel.IN_APP);
    assertThat(c.channel()).isEqualTo("#standup");
    assertThat(c.lookbackHours()).isEqualTo(48);
    assertThat(c.onlyWhenAtRisk()).isFalse();
    assertThat(c.useAi()).isFalse();
  }

  @Test
  @DisplayName("Unknown or missing channels are rejected with a useful message")
  void badChannels() {
    assertThatThrownBy(() -> parse("{\"channels\":[\"EMAIL\"]}"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("SLACK, TEAMS or IN_APP");
    assertThatThrownBy(() -> parse("{\"channels\":[]}"))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("at least one channel");
    assertThatThrownBy(() -> parse("{\"channels\":\"SLACK\"}")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("The look-back window must be 1–168 whole hours")
  void lookbackBounds() {
    assertThat(parse("{\"lookbackHours\":1}").lookbackHours()).isEqualTo(1);
    assertThat(parse("{\"lookbackHours\":168}").lookbackHours()).isEqualTo(168);
    assertThatThrownBy(() -> parse("{\"lookbackHours\":0}")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parse("{\"lookbackHours\":169}")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parse("{\"lookbackHours\":\"soon\"}")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("A channel override must look like a channel name")
  void channelName() {
    assertThat(parse("{\"channel\":\"eng-standup\"}").channel()).isEqualTo("eng-standup");
    assertThatThrownBy(() -> parse("{\"channel\":\"<!channel>\"}")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parse("{\"channel\":\"two words\"}")).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("Settings that are not a JSON object are rejected")
  void notAnObject() {
    assertThatThrownBy(() -> parse("[1,2]")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> parse("nope")).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("JSON");
  }
}
