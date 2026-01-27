package com.github.farzadsedaghatbin.shipflow.config.vectorstore;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers.ChromaVectorStoreProvider;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers.InMemoryVectorStoreProvider;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers.QdrantVectorStoreProvider;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VectorStoreProviderFactory.
 */
class VectorStoreProviderFactoryTest {

    private VectorStoreProviderFactory factory;

    @BeforeEach
    void setUp() {
        // Create factory with test providers
        List<VectorStoreProvider> providers = List.of(
                new InMemoryVectorStoreProvider(),
                new QdrantVectorStoreProvider(),
                new ChromaVectorStoreProvider()
        );
        factory = new VectorStoreProviderFactory(providers);
    }

    @Test
    void shouldRegisterAllProviders() {
        assertTrue(factory.isProviderAvailable(VectorStoreProviderType.IN_MEMORY));
        assertTrue(factory.isProviderAvailable(VectorStoreProviderType.QDRANT));
        assertTrue(factory.isProviderAvailable(VectorStoreProviderType.CHROMA));
    }

    @Test
    void shouldCheckProviderAvailabilityByString() {
        assertTrue(factory.isProviderAvailable("in-memory"));
        assertTrue(factory.isProviderAvailable("qdrant"));
        assertTrue(factory.isProviderAvailable("chroma"));
        assertFalse(factory.isProviderAvailable("unknown"));
    }

    @Test
    void getAvailableProviders_shouldReturnAllTypes() {
        var available = factory.getAvailableProviders();
        assertTrue(available.contains(VectorStoreProviderType.IN_MEMORY));
        assertTrue(available.contains(VectorStoreProviderType.QDRANT));
        assertTrue(available.contains(VectorStoreProviderType.CHROMA));
        assertEquals(3, available.size());
    }

    @Test
    void getProvider_shouldReturnCorrectProvider() {
        VectorStoreProvider inMemoryProvider = factory.getProvider(VectorStoreProviderType.IN_MEMORY);
        assertNotNull(inMemoryProvider);
        assertTrue(inMemoryProvider instanceof InMemoryVectorStoreProvider);

        VectorStoreProvider qdrantProvider = factory.getProvider(VectorStoreProviderType.QDRANT);
        assertNotNull(qdrantProvider);
        assertTrue(qdrantProvider instanceof QdrantVectorStoreProvider);

        VectorStoreProvider chromaProvider = factory.getProvider(VectorStoreProviderType.CHROMA);
        assertNotNull(chromaProvider);
        assertTrue(chromaProvider instanceof ChromaVectorStoreProvider);
    }

    @Test
    void getProvider_shouldReturnNullForUnregistered() {
        assertNull(factory.getProvider(VectorStoreProviderType.MILVUS));
        assertNull(factory.getProvider(VectorStoreProviderType.PINECONE));
    }

    @Test
    void createStore_shouldCreateInMemoryStore() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .collectionName("test_collection")
                .dimension(384)
                .build();

        EmbeddingStore<TextSegment> store = factory.createStore(VectorStoreProviderType.IN_MEMORY, config);
        assertNotNull(store);
    }

    @Test
    void createStore_shouldCreateStoreByStringType() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder()
                .collectionName("test_collection")
                .dimension(384)
                .build();

        EmbeddingStore<TextSegment> store = factory.createStore("in-memory", config);
        assertNotNull(store);
    }

    @Test
    void createStore_shouldThrowForUnavailableProvider() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();

        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class,
            () -> factory.createStore(VectorStoreProviderType.MILVUS, config)
        );
        assertTrue(ex.getMessage().contains("No provider registered"));
    }

    @Test
    void createStore_shouldThrowForUnknownProviderString() {
        VectorStoreProviderConfig config = VectorStoreProviderConfig.builder().build();

        assertThrows(
            IllegalArgumentException.class,
            () -> factory.createStore("unknown", config)
        );
    }

    @Test
    void getProvidersInfo_shouldReturnFormattedInfo() {
        String info = factory.getProvidersInfo();
        
        assertTrue(info.contains("Registered Vector Store Providers"));
        assertTrue(info.contains("in-memory"));
        assertTrue(info.contains("qdrant"));
        assertTrue(info.contains("chroma"));
        assertTrue(info.contains("InMemoryVectorStoreProvider"));
        assertTrue(info.contains("QdrantVectorStoreProvider"));
        assertTrue(info.contains("ChromaVectorStoreProvider"));
    }

    @Test
    void getProvidersInfo_shouldIndicateApiKeyRequirement() {
        String info = factory.getProvidersInfo();
        
        // Qdrant requires API key
        assertTrue(info.contains("[API Key Required]"));
    }

    @Test
    void factoryWithEmptyProviders_shouldHaveNoProviders() {
        VectorStoreProviderFactory emptyFactory = new VectorStoreProviderFactory(List.of());
        
        assertTrue(emptyFactory.getAvailableProviders().isEmpty());
        assertFalse(emptyFactory.isProviderAvailable("qdrant"));
    }

    @Test
    void factoryWithDuplicateProviders_shouldKeepLast() {
        // Create two in-memory providers (simulating duplicate registration)
        InMemoryVectorStoreProvider provider1 = new InMemoryVectorStoreProvider();
        InMemoryVectorStoreProvider provider2 = new InMemoryVectorStoreProvider();
        
        VectorStoreProviderFactory factoryWithDuplicates = new VectorStoreProviderFactory(
            List.of(provider1, provider2)
        );
        
        // Should still work, last one wins
        assertTrue(factoryWithDuplicates.isProviderAvailable(VectorStoreProviderType.IN_MEMORY));
        assertEquals(1, factoryWithDuplicates.getAvailableProviders().size());
    }
}
