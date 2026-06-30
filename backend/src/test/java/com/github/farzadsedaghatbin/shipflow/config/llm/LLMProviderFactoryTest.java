package com.github.farzadsedaghatbin.shipflow.config.llm;

import static org.junit.jupiter.api.Assertions.*;

import com.github.farzadsedaghatbin.shipflow.config.AirGappedProperties;
import com.github.farzadsedaghatbin.shipflow.config.llm.providers.OllamaLLMProvider;
import com.github.farzadsedaghatbin.shipflow.config.llm.providers.OpenAILLMProvider;
import com.github.farzadsedaghatbin.shipflow.config.llm.providers.RunPodLLMProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for LLMProviderFactory. */
class LLMProviderFactoryTest {

  private LLMProviderFactory factory;
  private AirGappedProperties airGappedProperties;

  @BeforeEach
  void setUp() {
    // Create factory with test providers
    List<LLMProvider> providers = List.of(new OllamaLLMProvider(), new OpenAILLMProvider(),
        new RunPodLLMProvider());
    airGappedProperties = new AirGappedProperties();
    factory = new LLMProviderFactory(providers, airGappedProperties);
  }

  private static AirGappedProperties props(boolean enabled) {
    AirGappedProperties p = new AirGappedProperties();
    p.setEnabled(enabled);
    return p;
  }

  @Test
  void shouldRegisterAllProviders() {
    assertTrue(factory.isProviderAvailable(LLMProviderType.OLLAMA));
    assertTrue(factory.isProviderAvailable(LLMProviderType.OPENAI));
    assertTrue(factory.isProviderAvailable(LLMProviderType.RUNPOD));
  }

  @Test
  void shouldCheckProviderAvailabilityByString() {
    assertTrue(factory.isProviderAvailable("ollama"));
    assertTrue(factory.isProviderAvailable("openai"));
    assertTrue(factory.isProviderAvailable("runpod"));
    assertFalse(factory.isProviderAvailable("unknown"));
  }

  @Test
  void getAvailableProviders_shouldReturnAllTypes() {
    var available = factory.getAvailableProviders();
    assertTrue(available.contains(LLMProviderType.OLLAMA));
    assertTrue(available.contains(LLMProviderType.OPENAI));
    assertTrue(available.contains(LLMProviderType.RUNPOD));
    assertEquals(3, available.size());
  }

  @Test
  void getProvider_shouldReturnCorrectProvider() {
    LLMProvider ollamaProvider = factory.getProvider(LLMProviderType.OLLAMA);
    assertNotNull(ollamaProvider);
    assertTrue(ollamaProvider instanceof OllamaLLMProvider);

    LLMProvider openaiProvider = factory.getProvider(LLMProviderType.OPENAI);
    assertNotNull(openaiProvider);
    assertTrue(openaiProvider instanceof OpenAILLMProvider);
  }

  @Test
  void getProvider_shouldReturnNullForUnregistered() {
    assertNull(factory.getProvider(LLMProviderType.ANTHROPIC));
  }

  @Test
  void createModel_shouldCreateOllamaModel() {
    LLMProviderConfig config = LLMProviderConfig.builder().baseUrl("http://localhost:11434")
        .modelName("mistral:instruct").timeout(Duration.ofSeconds(180)).temperature(0.3).build();

    ChatLanguageModel model = factory.createModel(LLMProviderType.OLLAMA, config);
    assertNotNull(model);
  }

  @Test
  void createModel_shouldCreateOpenAIModel() {
    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("sk-test-key-12345").modelName("gpt-4o-mini")
        .timeout(Duration.ofSeconds(120)).temperature(0.3).build();

    ChatLanguageModel model = factory.createModel(LLMProviderType.OPENAI, config);
    assertNotNull(model);
  }

  @Test
  void createModel_shouldThrowForUnavailableProvider() {
    LLMProviderConfig config = LLMProviderConfig.builder().build();

    IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
        () -> factory.createModel(LLMProviderType.ANTHROPIC, config));
    assertTrue(ex.getMessage().contains("No provider registered"));
  }

  @Test
  void createModel_shouldThrowForInvalidConfig() {
    // OpenAI requires API key
    LLMProviderConfig config = LLMProviderConfig.builder().modelName("gpt-4o-mini").build();

    assertThrows(IllegalArgumentException.class, () -> factory.createModel(LLMProviderType.OPENAI, config));
  }

  @Test
  void createModelByString_shouldWork() {
    LLMProviderConfig config = LLMProviderConfig.builder().baseUrl("http://localhost:11434")
        .modelName("mistral:instruct").build();

    ChatLanguageModel model = factory.createModel("ollama", config);
    assertNotNull(model);
  }

  @Test
  void createModelByString_shouldThrowForInvalidString() {
    LLMProviderConfig config = LLMProviderConfig.builder().build();

    assertThrows(IllegalArgumentException.class, () -> factory.createModel("invalid-provider", config));
  }

  @Test
  void createModel_airGapped_shouldThrowForNonLocalProvider() {
    List<LLMProvider> providers = List.of(new OllamaLLMProvider(), new OpenAILLMProvider(),
        new RunPodLLMProvider());
    LLMProviderFactory airGappedFactory = new LLMProviderFactory(providers, props(true));

    LLMProviderConfig config = LLMProviderConfig.builder().apiKey("sk-test-key-12345")
        .modelName("gpt-4o-mini").timeout(Duration.ofSeconds(120)).temperature(0.3).build();

    IllegalStateException ex = assertThrows(IllegalStateException.class,
        () -> airGappedFactory.createModel(LLMProviderType.OPENAI, config));
    assertTrue(ex.getMessage().contains("Air-gapped"));
  }

  @Test
  void createModel_airGapped_shouldAllowLocalProvider() {
    List<LLMProvider> providers = List.of(new OllamaLLMProvider(), new OpenAILLMProvider());
    LLMProviderFactory airGappedFactory = new LLMProviderFactory(providers, props(true));

    LLMProviderConfig config = LLMProviderConfig.builder().baseUrl("http://localhost:11434")
        .modelName("mistral:instruct").timeout(Duration.ofSeconds(180)).temperature(0.3).build();

    ChatLanguageModel model = airGappedFactory.createModel(LLMProviderType.OLLAMA, config);
    assertNotNull(model);
  }

  @Test
  void getProviderInfo_shouldReturnFormattedInfo() {
    String info = factory.getProviderInfo();
    assertTrue(info.contains("Available LLM Providers"));
    assertTrue(info.contains("ollama"));
    assertTrue(info.contains("openai"));
    assertTrue(info.contains("runpod"));
    assertTrue(info.contains("Requires API Key"));
  }
}
