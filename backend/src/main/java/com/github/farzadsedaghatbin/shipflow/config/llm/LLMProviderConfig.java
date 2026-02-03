package com.github.farzadsedaghatbin.shipflow.config.llm;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Configuration container for LLM providers. Supports common settings and
 * provider-specific extra parameters.
 */
public class LLMProviderConfig {

  private String baseUrl;
  private String apiKey;
  private String modelName;
  private Duration timeout;
  private Double temperature;
  private Integer maxTokens;
  private final Map<String, Object> extraParams;

  private LLMProviderConfig(Builder builder) {
    this.baseUrl = builder.baseUrl;
    this.apiKey = builder.apiKey;
    this.modelName = builder.modelName;
    this.timeout = builder.timeout;
    this.temperature = builder.temperature;
    this.maxTokens = builder.maxTokens;
    this.extraParams = new HashMap<>(builder.extraParams);
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters
  public String getBaseUrl() {
    return baseUrl;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getModelName() {
    return modelName;
  }

  public Duration getTimeout() {
    return timeout;
  }

  public Double getTemperature() {
    return temperature;
  }

  public Integer getMaxTokens() {
    return maxTokens;
  }

  public Map<String, Object> getExtraParams() {
    return new HashMap<>(extraParams);
  }

  @SuppressWarnings("unchecked")
  public <T> T getExtraParam(String key, T defaultValue) {
    Object value = extraParams.get(key);
    if (value == null) {
      return defaultValue;
    }
    return (T) value;
  }

  public static class Builder {
    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Duration timeout = Duration.ofSeconds(120);
    private Double temperature = 0.3;
    private Integer maxTokens = 2048;
    private final Map<String, Object> extraParams = new HashMap<>();

    public Builder baseUrl(String baseUrl) {
      this.baseUrl = baseUrl;
      return this;
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder modelName(String modelName) {
      this.modelName = modelName;
      return this;
    }

    public Builder timeout(Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder temperature(Double temperature) {
      this.temperature = temperature;
      return this;
    }

    public Builder maxTokens(Integer maxTokens) {
      this.maxTokens = maxTokens;
      return this;
    }

    public Builder extraParam(String key, Object value) {
      this.extraParams.put(key, value);
      return this;
    }

    public Builder extraParams(Map<String, Object> params) {
      this.extraParams.putAll(params);
      return this;
    }

    public LLMProviderConfig build() {
      return new LLMProviderConfig(this);
    }
  }

  @Override
  public String toString() {
    return "LLMProviderConfig{" + "baseUrl='" + baseUrl + '\'' + ", modelName='" + modelName + '\'' + ", timeout="
        + timeout + ", temperature=" + temperature + ", maxTokens=" + maxTokens + ", extraParams="
        + extraParams.keySet() + '}';
  }
}
