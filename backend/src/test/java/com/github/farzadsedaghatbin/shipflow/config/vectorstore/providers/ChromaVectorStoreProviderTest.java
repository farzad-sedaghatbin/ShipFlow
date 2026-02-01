package com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers;

import static org.junit.jupiter.api.Assertions.*;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Tests for ChromaVectorStoreProvider. */
class ChromaVectorStoreProviderTest {

  private ChromaVectorStoreProvider provider;

  @BeforeEach
  void setUp() {
    provider = new ChromaVectorStoreProvider();
  }

  @Test
  void getProviderType_shouldReturnChroma() {
    assertEquals(VectorStoreProviderType.CHROMA, provider.getProviderType());
  }

  @Test
  void requiresApiKey_shouldReturnFalse() {
    assertFalse(provider.requiresApiKey());
  }

  @Test
  void requiresUrl_shouldReturnTrue() {
    assertTrue(provider.requiresUrl());
  }

  @Test
  void getDefaultPort_shouldReturn8000() {
    assertEquals(8000, provider.getDefaultPort());
  }

  @Test
  void getDescription_shouldContainChromaDB() {
    String description = provider.getDescription();
    assertTrue(description.contains("ChromaDB") || description.contains("Chroma"));
  }

  @Test
  void validateConfig_shouldNotThrowForValidConfig() {
    VectorStoreProviderConfig config =
        VectorStoreProviderConfig.builder()
            .url("http://localhost:8000")
            .collectionName("test_collection")
            .build();

    assertDoesNotThrow(() -> provider.validateConfig(config));
  }

  @Test
  void validateConfig_shouldThrowForMissingCollectionName() {
    VectorStoreProviderConfig config =
        VectorStoreProviderConfig.builder().url("http://localhost:8000").collectionName("").build();

    assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
  }

  @Test
  void validateConfig_shouldThrowForNullCollectionName() {
    VectorStoreProviderConfig config =
        VectorStoreProviderConfig.builder()
            .url("http://localhost:8000")
            .collectionName(null)
            .build();

    assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
  }

  // Note: createStore tests require a running ChromaDB instance
  // Integration tests would be needed for actual store creation
}
