package com.github.farzadsedaghatbin.shipflow.config.vectorstore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for VectorStoreProviderType enum.
 */
class VectorStoreProviderTypeTest {

    @Test
    void fromConfigValue_shouldReturnCorrectType() {
        assertEquals(VectorStoreProviderType.IN_MEMORY, VectorStoreProviderType.fromConfigValue("in-memory"));
        assertEquals(VectorStoreProviderType.QDRANT, VectorStoreProviderType.fromConfigValue("qdrant"));
        assertEquals(VectorStoreProviderType.CHROMA, VectorStoreProviderType.fromConfigValue("chroma"));
        assertEquals(VectorStoreProviderType.MILVUS, VectorStoreProviderType.fromConfigValue("milvus"));
        assertEquals(VectorStoreProviderType.PINECONE, VectorStoreProviderType.fromConfigValue("pinecone"));
        assertEquals(VectorStoreProviderType.WEAVIATE, VectorStoreProviderType.fromConfigValue("weaviate"));
    }

    @Test
    void fromConfigValue_shouldBeCaseInsensitive() {
        assertEquals(VectorStoreProviderType.QDRANT, VectorStoreProviderType.fromConfigValue("QDRANT"));
        assertEquals(VectorStoreProviderType.QDRANT, VectorStoreProviderType.fromConfigValue("Qdrant"));
        assertEquals(VectorStoreProviderType.IN_MEMORY, VectorStoreProviderType.fromConfigValue("IN-MEMORY"));
    }

    @Test
    void fromConfigValue_shouldHandleWhitespace() {
        assertEquals(VectorStoreProviderType.QDRANT, VectorStoreProviderType.fromConfigValue("  qdrant  "));
        assertEquals(VectorStoreProviderType.CHROMA, VectorStoreProviderType.fromConfigValue(" chroma "));
    }

    @Test
    void fromConfigValue_shouldThrowExceptionForNullOrEmpty() {
        assertThrows(IllegalArgumentException.class, () -> VectorStoreProviderType.fromConfigValue(null));
        assertThrows(IllegalArgumentException.class, () -> VectorStoreProviderType.fromConfigValue(""));
        assertThrows(IllegalArgumentException.class, () -> VectorStoreProviderType.fromConfigValue("   "));
    }

    @Test
    void fromConfigValue_shouldThrowExceptionForUnknownProvider() {
        IllegalArgumentException ex = assertThrows(
            IllegalArgumentException.class, 
            () -> VectorStoreProviderType.fromConfigValue("unknown-provider")
        );
        assertTrue(ex.getMessage().contains("Unknown vector store provider"));
        assertTrue(ex.getMessage().contains("qdrant"));
    }

    @Test
    void getAvailableProvidersInfo_shouldReturnAllProviders() {
        String info = VectorStoreProviderType.getAvailableProvidersInfo();
        assertTrue(info.contains("in-memory"));
        assertTrue(info.contains("qdrant"));
        assertTrue(info.contains("chroma"));
        assertTrue(info.contains("milvus"));
        assertTrue(info.contains("pinecone"));
        assertTrue(info.contains("weaviate"));
    }

    @Test
    void getConfigValue_shouldReturnCorrectValue() {
        assertEquals("in-memory", VectorStoreProviderType.IN_MEMORY.getConfigValue());
        assertEquals("qdrant", VectorStoreProviderType.QDRANT.getConfigValue());
        assertEquals("chroma", VectorStoreProviderType.CHROMA.getConfigValue());
    }

    @Test
    void getDisplayName_shouldReturnCorrectName() {
        assertEquals("In-Memory (Development/Testing)", VectorStoreProviderType.IN_MEMORY.getDisplayName());
        assertEquals("Qdrant (Production Recommended)", VectorStoreProviderType.QDRANT.getDisplayName());
        assertEquals("ChromaDB", VectorStoreProviderType.CHROMA.getDisplayName());
    }

    @Test
    void allTypesHaveConfigValues() {
        for (VectorStoreProviderType type : VectorStoreProviderType.values()) {
            assertNotNull(type.getConfigValue());
            assertFalse(type.getConfigValue().isEmpty());
        }
    }

    @Test
    void allTypesHaveDisplayNames() {
        for (VectorStoreProviderType type : VectorStoreProviderType.values()) {
            assertNotNull(type.getDisplayName());
            assertFalse(type.getDisplayName().isEmpty());
        }
    }
}
