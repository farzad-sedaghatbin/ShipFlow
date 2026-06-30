package com.github.farzadsedaghatbin.shipflow.config.llm;

/**
 * Enumeration of supported LLM providers. Add new providers here to extend the
 * system.
 */
public enum LLMProviderType {
  OLLAMA("ollama", "Ollama (Local/Self-hosted)"), RUNPOD("runpod", "RunPod Serverless GPU"), OPENAI("openai",
      "OpenAI ChatGPT"), ANTHROPIC("anthropic", "Anthropic Claude"), GOOGLE("google",
          "Google Gemini"), AZURE_OPENAI("azure-openai", "Azure OpenAI");

  private final String configValue;
  private final String displayName;

  LLMProviderType(String configValue, String displayName) {
    this.configValue = configValue;
    this.displayName = displayName;
  }

  public String getConfigValue() {
    return configValue;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Whether this provider runs entirely locally with no external network egress, making it safe for
   * air-gapped deployments.
   *
   * <p>Only {@link #OLLAMA} qualifies: it needs no API key and points at a configurable, typically
   * local, base URL. Every other provider (OpenAI, Anthropic, RunPod, Google, Azure OpenAI) calls a
   * vendor-hosted endpoint.
   *
   * @return true if the provider is air-gap-safe (local)
   */
  public boolean isLocal() {
    return this == OLLAMA;
  }

  /**
   * Parse provider type from configuration string.
   *
   * @param value
   *            the configuration value
   * @return the matching provider type
   * @throws IllegalArgumentException
   *             if no matching provider is found
   */
  public static LLMProviderType fromConfigValue(String value) {
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalArgumentException("Provider type cannot be null or empty");
    }

    String normalizedValue = value.trim().toLowerCase();
    for (LLMProviderType type : values()) {
      if (type.configValue.equals(normalizedValue)) {
        return type;
      }
    }

    throw new IllegalArgumentException(
        "Unknown LLM provider: '" + value + "'. Supported providers: " + getSupportedProviders());
  }

  /** Get a comma-separated list of supported provider config values. */
  public static String getSupportedProviders() {
    StringBuilder sb = new StringBuilder();
    for (LLMProviderType type : values()) {
      if (sb.length() > 0) {
        sb.append(", ");
      }
      sb.append(type.configValue);
    }
    return sb.toString();
  }
}
