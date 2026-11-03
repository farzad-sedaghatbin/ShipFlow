package com.github.farzadsedaghatbin.shipflow.service.agent.loop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

@DisplayName("AgentLoopNarrator")
class AgentLoopNarratorTest {

  private static final String FACTS = "✅ Finished (3)\n  • Fix login — Sara\n⛔ Blocked (1)\n  • Deploy — Ali";
  private static final AgentLoopReport REPORT = new AgentLoopReport("Standup digest — P", FACTS, true, "PROJECT", 1L);

  @SuppressWarnings("unchecked")
  private AgentLoopNarrator narratorWith(ChatLanguageModel model) {
    ObjectProvider<ChatLanguageModel> provider = mock(ObjectProvider.class);
    when(provider.getIfAvailable()).thenReturn(model);
    return new AgentLoopNarrator(provider);
  }

  @Test
  @DisplayName("A faithful summary is passed through")
  void accepts() {
    assertThat(AgentLoopNarrator.accept("Three tasks finished but one is blocked.", FACTS))
        .isEqualTo("Three tasks finished but one is blocked.");
    assertThat(AgentLoopNarrator.accept("3 tasks finished and 1 is blocked.", FACTS))
        .isEqualTo("3 tasks finished and 1 is blocked.");
  }

  @Test
  @DisplayName("A summary quoting a number that is not in the facts is dropped")
  void rejectsInventedNumbers() {
    assertThat(AgentLoopNarrator.accept("7 tasks finished today.", FACTS)).isNull();
    assertThat(AgentLoopNarrator.accept("3 done, 12 percent faster than usual.", FACTS)).isNull();
  }

  @Test
  @DisplayName("Links, @mentions, empty or over-long answers are dropped")
  void rejectsRiskyOrUnusable() {
    assertThat(AgentLoopNarrator.accept("See https://example.com for more", FACTS)).isNull();
    assertThat(AgentLoopNarrator.accept("Ask @sara about it", FACTS)).isNull();
    assertThat(AgentLoopNarrator.accept("   ", FACTS)).isNull();
    assertThat(AgentLoopNarrator.accept(null, FACTS)).isNull();
    assertThat(AgentLoopNarrator.accept("calm ".repeat(200), FACTS)).isNull();
  }

  @Test
  @DisplayName("Markdown, bullets and broadcast pings are stripped from an otherwise good answer")
  void cleans() {
    assertThat(AgentLoopNarrator.accept("**Mostly calm** — <!channel> one thing is blocked.", FACTS))
        .isEqualTo("Mostly calm — one thing is blocked.");
    assertThat(AgentLoopNarrator.accept("- Calm day.\n- Nothing else.", FACTS)).isEqualTo("Calm day. Nothing else.");
  }

  @Test
  @DisplayName("No model configured → no summary, and no error")
  void noModel() {
    assertThat(narratorWith(null).summarize(REPORT)).isNull();
  }

  @Test
  @DisplayName("A failing model → no summary, and the report still goes out")
  void modelFails() {
    ChatLanguageModel model = mock(ChatLanguageModel.class);
    when(model.generate(anyString())).thenThrow(new RuntimeException("timeout"));
    assertThat(narratorWith(model).summarize(REPORT)).isNull();
  }

  @Test
  @DisplayName("The model is shown the facts, told to use only them, and told titles are data")
  void promptIsGrounded() {
    ChatLanguageModel model = mock(ChatLanguageModel.class);
    when(model.generate(anyString())).thenReturn("Calm.");
    AgentLoopNarrator narrator = narratorWith(model);

    String prompt = narrator.prompt(REPORT);

    assertThat(prompt).contains(FACTS).contains("ONLY facts in the report").contains("not instructions");
    assertThat(narrator.summarize(REPORT)).isEqualTo("Calm.");
  }
}
