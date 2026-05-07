package com.github.farzadsedaghatbin.shipflow.config.vectorstore;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration container for vector store providers. Supports common settings
 * and provider-specific extra parameters.
 *
 * <p>
 * Common settings:
 *
 * <ul>
 * <li>url: Base URL for the vector store service
 * <li>apiKey: API key for authentication (if required)
 * <li>collectionName: Name of the collection/index to use
 * <li>dimension: Vector dimension (default: 384 for all-MiniLM-L6-v2)
 * </ul>
 */
public class VectorStoreProviderConfig {

  /** Default vector dimension for all-MiniLM-L6-v2 embedding model */
  public static final int DEFAULT_DIMENSION = 384;

  private String url;
  private String host;
  private int port;
  private String apiKey;
  private String collectionName;
  private int dimension;
  private final Map<String, Object> extraParams;

  private VectorStoreProviderConfig(Builder builder) {
    this.url = builder.url;
    this.host = builder.host;
    this.port = builder.port;
    this.apiKey = builder.apiKey;
    this.collectionName = builder.collectionName;
    this.dimension = builder.dimension;
    this.extraParams = new HashMap<>(builder.extraParams);
  }

  public static Builder builder() {
    return new Builder();
  }

  // Getters
  public String getUrl() {
    return url;
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public String getApiKey() {
    return apiKey;
  }

  public String getCollectionName() {
    return collectionName;
  }

  public int getDimension() {
    return dimension;
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

  /** Check if API key is configured. */
  public boolean hasApiKey() {
    return apiKey != null && !apiKey.trim().isEmpty();
  }

  /**
   * Get the effective URL, either from url field or constructed from host:port.
   */
  public String getEffectiveUrl() {
    if (url != null && !url.trim().isEmpty()) {
      return url;
    }
    if (host != null && !host.trim().isEmpty()) {
      return "http://" + host + ":" + port;
    }
    return null;
  }

  @Override
  public String toString() {
    return "VectorStoreProviderConfig{" + "url='" + url + '\'' + ", host='" + host + '\'' + ", port=" + port
        + ", apiKey='" + (apiKey != null ? "****" : "null") + '\'' + ", collectionName='" + collectionName
        + '\'' + ", dimension=" + dimension + '}';
  }

  public static class Builder {
    private String url;
    private String host = "localhost";
    private int port = 6333; // Qdrant default port
    private String apiKey;
    private String collectionName = "shipflow_knowledge";
    private int dimension = DEFAULT_DIMENSION;
    private final Map<String, Object> extraParams = new HashMap<>();

    public Builder url(String url) {
      this.url = url;
      return this;
    }

    public Builder host(String host) {
      this.host = host;
      return this;
    }

    public Builder port(int port) {
      this.port = port;
      return this;
    }

    public Builder apiKey(String apiKey) {
      this.apiKey = apiKey;
      return this;
    }

    public Builder collectionName(String collectionName) {
      this.collectionName = collectionName;
      return this;
    }

    public Builder dimension(int dimension) {
      this.dimension = dimension;
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

    public VectorStoreProviderConfig build() {
      return new VectorStoreProviderConfig(this);
    }
  }
}
