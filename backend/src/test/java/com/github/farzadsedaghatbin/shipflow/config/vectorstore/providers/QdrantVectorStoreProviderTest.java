package com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for QdrantVectorStoreProvider.
 */
class QdrantVectorStoreProviderTest {

    private QdrantVectorStoreProvider provider;

    @BeforeEach
    void setUp() {
        provider = new QdrantVectorStoreProvider();
    }

    @Test
    void getProviderType_shouldReturnQdrant() {
        assertEquals(VectorStoreProviderType.QDRANT, provider.getProviderType());
    }

    @Test
    void requiresApiKey_shouldReturnTrue() {
        assertTrue(provider.requiresApiKey());
    }

    @Test
    void requiresUrl_shouldReturnTrue() {
        assertTrue(provider.requiresUrl());
    }

    @Test
    void getDefaultPort_shouldReturn6334() {
        assertEquals(6334, provider.getDefaultPort());
    }

    @Test
    void getDescription_shouldContainProduction() {
        String description = provider.getDescription();
        assertTrue(description.contains("Production") || description.contains("production"));
        assertTrue(description.contains("Qdrant"));
    }

    @Test
    void validateConfig_shouldNotThrowForValidConfig() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .host("localhost")
                .port(6334)
                .apiKey("test-api-key")
                .collectionName("test_collection")
                .dimension(384)
                .build();

        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    void validateConfig_shouldNotThrowWithoutApiKey() {
        // Should warn but not throw - API key is recommended but not required
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .host("localhost")
                .port(6334)
                .collectionName("test_collection")
                .dimension(384)
                .build();

        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    void validateConfig_shouldThrowForMissingCollectionName() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .host("localhost")
                .port(6334)
                .collectionName("")
                .dimension(384)
                .build();

        assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
    }

    @Test
    void validateConfig_shouldThrowForNullCollectionName() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .host("localhost")
                .port(6334)
                .collectionName(null)
                .dimension(384)
                .build();

        assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
    }

    @Test
    void validateConfig_shouldThrowForInvalidDimension() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .host("localhost")
                .port(6334)
                .collectionName("test")
                .dimension(0)
                .build();

        assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
    }

    @Test
    void validateConfig_shouldThrowForNegativeDimension() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .host("localhost")
                .port(6334)
                .collectionName("test")
                .dimension(-1)
                .build();

        assertThrows(IllegalArgumentException.class, () -> provider.validateConfig(config));
    }

    // Note: createStore tests require a running Qdrant instance
    // Integration tests would be needed for actual store creation
}
