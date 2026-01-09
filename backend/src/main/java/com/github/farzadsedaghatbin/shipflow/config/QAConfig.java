package com.github.farzadsedaghatbin.shipflow.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Q&A feature including embeddings and vector store.
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "app.features.qa.enabled", havingValue = "true")
public class QAConfig {

    @Value("${app.qa.vector-store.type:in-memory}")
    private String vectorStoreType;

    @Value("${app.qa.chromadb.url:http://localhost:8000}")
    private String chromaDbUrl;

    @Value("${app.qa.chromadb.collection:shipflow_knowledge}")
    private String chromaDbCollection;

    @Value("${app.qa.retrieval.top-k:5}")
    private int topK;

    @Value("${app.qa.chunking.max-tokens:512}")
    private int maxTokens;

    @Value("${app.qa.chunking.overlap:50}")
    private int chunkOverlap;

    /**
     * Creates the embedding model bean using all-MiniLM-L6-v2.
     * This model is fast, accurate, and lightweight for local inference.
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing embedding model: all-MiniLM-L6-v2");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Creates the embedding store bean based on configuration.
     * Uses in-memory store for development, ChromaDB for production.
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        if ("chromadb".equalsIgnoreCase(vectorStoreType)) {
            log.info("Initializing ChromaDB embedding store - URL: {}, Collection: {}", 
                    chromaDbUrl, chromaDbCollection);
            return ChromaEmbeddingStore.builder()
                    .baseUrl(chromaDbUrl)
                    .collectionName(chromaDbCollection)
                    .build();
        } else {
            log.info("Initializing in-memory embedding store for development");
            return new InMemoryEmbeddingStore<>();
        }
    }

    public String getVectorStoreType() {
        return vectorStoreType;
    }

    public int getTopK() {
        return topK;
    }

    public int getMaxTokens() {
        return maxTokens;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }
}
