package com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for InMemoryVectorStoreProvider.
 */
class InMemoryVectorStoreProviderTest {

    private InMemoryVectorStoreProvider provider;

    @BeforeEach
    void setUp() {
        provider = new InMemoryVectorStoreProvider();
    }

    @Test
    void getProviderType_shouldReturnInMemory() {
        assertEquals(VectorStoreProviderType.IN_MEMORY, provider.getProviderType());
    }

    @Test
    void requiresApiKey_shouldReturnFalse() {
        assertFalse(provider.requiresApiKey());
    }

    @Test
    void requiresUrl_shouldReturnFalse() {
        assertFalse(provider.requiresUrl());
    }

    @Test
    void getDefaultPort_shouldReturnZero() {
        assertEquals(0, provider.getDefaultPort());
    }

    @Test
    void getDescription_shouldContainDevelopment() {
        String description = provider.getDescription();
        assertTrue(description.contains("Development") || description.contains("Testing"));
        assertTrue(description.contains("Non-persistent") || description.contains("non-persistent"));
    }

    @Test
    void createStore_shouldCreateStore() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .collectionName("test_collection")
                .dimension(384)
                .build();

        EmbeddingStore<TextSegment> store = provider.createStore(config);
        assertNotNull(store);
    }

    @Test
    void createStore_shouldWorkWithMinimalConfig() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();

        EmbeddingStore<TextSegment> store = provider.createStore(config);
        assertNotNull(store);
    }

    @Test
    void validateConfig_shouldNotThrowForAnyConfig() {
        // In-memory doesn't require any validation
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }

    @Test
    void validateConfig_shouldNotThrowForEmptyConfig() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .collectionName("")
                .build();
        assertDoesNotThrow(() -> provider.validateConfig(config));
    }
}
