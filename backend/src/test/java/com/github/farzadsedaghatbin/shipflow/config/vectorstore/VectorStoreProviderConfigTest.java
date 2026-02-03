package com.github.farzadsedaghatbin.shipflow.config.vectorstore;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for VectorStoreProviderConfig. */
class VectorStoreProviderConfigTest {

  @Test
  void builder_shouldCreateConfigWithAllFields() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().url("http://localhost:6333")
        .host("localhost").port(6334).apiKey("test-api-key").collectionName("test_collection").dimension(384)
        .build();

    assertEquals("http://localhost:6333", config.getUrl());
    assertEquals("localhost", config.getHost());
    assertEquals(6334, config.getPort());
    assertEquals("test-api-key", config.getApiKey());
    assertEquals("test_collection", config.getCollectionName());
    assertEquals(384, config.getDimension());
  }

  @Test
  void builder_shouldUseDefaultValues() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();

    assertEquals("localhost", config.getHost());
    assertEquals(6333, config.getPort());
    assertEquals("shipflow_knowledge", config.getCollectionName());
    assertEquals(VectorStoreProviderConfig.DEFAULT_DIMENSION, config.getDimension());
  }

  @Test
  void defaultDimension_shouldBe384() {
    assertEquals(384, VectorStoreProviderConfig.DEFAULT_DIMENSION);
  }

  @Test
  void hasApiKey_shouldReturnTrueWhenSet() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().apiKey("my-api-key").build();

    assertTrue(config.hasApiKey());
  }

  @Test
  void hasApiKey_shouldReturnFalseWhenNull() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();

    assertFalse(config.hasApiKey());
  }

  @Test
  void hasApiKey_shouldReturnFalseWhenEmpty() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().apiKey("").build();

    assertFalse(config.hasApiKey());
  }

  @Test
  void hasApiKey_shouldReturnFalseWhenBlank() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().apiKey("   ").build();

    assertFalse(config.hasApiKey());
  }

  @Test
  void getEffectiveUrl_shouldReturnUrlWhenSet() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().url("http://custom-url:8080")
        .host("localhost").port(6333).build();

    assertEquals("http://custom-url:8080", config.getEffectiveUrl());
  }

  @Test
  void getEffectiveUrl_shouldConstructFromHostPortWhenUrlNotSet() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().host("qdrant-server").port(6333).build();

    assertEquals("http://qdrant-server:6333", config.getEffectiveUrl());
  }

  @Test
  void getEffectiveUrl_shouldReturnNullWhenNeitherSet() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().host("").build();

    assertNull(config.getEffectiveUrl());
  }

  @Test
  void extraParams_shouldStoreAndRetrieveValues() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().extraParam("useTls", true)
        .extraParam("timeout", 30).extraParam("custom", "value").build();

    assertEquals(true, config.getExtraParam("useTls", false));
    assertEquals(30, config.getExtraParam("timeout", 0));
    assertEquals("value", config.getExtraParam("custom", "default"));
  }

  @Test
  void extraParams_shouldReturnDefaultWhenNotSet() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();

    assertEquals("default", config.getExtraParam("notSet", "default"));
    assertEquals(42, config.getExtraParam("notSet", 42));
    assertEquals(false, config.getExtraParam("notSet", false));
  }

  @Test
  void extraParams_shouldSupportBulkAdd() {
    Map<String, Object> params = Map.of("key1", "value1", "key2", 123, "key3", true);

    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().extraParams(params).build();

    assertEquals("value1", config.getExtraParam("key1", null));
    assertEquals(Integer.valueOf(123), config.getExtraParam("key2", null));
    assertEquals(true, config.getExtraParam("key3", null));
  }

  @Test
  void getExtraParams_shouldReturnCopy() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().extraParam("key", "value").build();

    Map<String, Object> params = config.getExtraParams();
    params.put("newKey", "newValue");

    // Original should be unchanged
    assertNull(config.getExtraParam("newKey", null));
  }

  @Test
  void toString_shouldMaskApiKey() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().apiKey("super-secret-key").build();

    String str = config.toString();
    assertTrue(str.contains("****"));
    assertFalse(str.contains("super-secret-key"));
  }

  @Test
  void toString_shouldShowNullApiKeyAsNull() {
    VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();

    String str = config.toString();
    assertTrue(str.contains("apiKey='null'"));
  }
}
