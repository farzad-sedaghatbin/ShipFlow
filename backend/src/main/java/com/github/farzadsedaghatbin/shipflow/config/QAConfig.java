package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderFactory;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for the Q&A feature including embeddings and vector store.
 * 
 * <p>The vector store is now pluggable - supports multiple providers:
 * <ul>
 *   <li>in-memory: For development and testing (default in dev profile)</li>
 *   <li>qdrant: Production recommended (default in prod profile)</li>
 *   <li>chroma: Alternative option</li>
 *   <li>milvus, pinecone, weaviate: Future/optional support</li>
 * </ul>
 * 
 * @see VectorStoreProviderFactory
 * @see VectorStoreProviderType
 */
@Configuration
@Slf4j
@ConditionalOnProperty(name = "app.features.qa.enabled", havingValue = "true")
public class QAConfig {

    // Vector Store Provider Settings
    @Value("${app.qa.vectorstore.provider:in-memory}")
    private String vectorStoreProvider;

    // Qdrant Settings
    @Value("${app.qa.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${app.qa.vectorstore.qdrant.port:6334}")
    private int qdrantPort;

    @Value("${app.qa.vectorstore.qdrant.api-key:}")
    private String qdrantApiKey;

    // ChromaDB Settings (legacy/alternative)
    @Value("${app.qa.vectorstore.chroma.url:http://localhost:8000}")
    private String chromaUrl;

    // Common Settings
    @Value("${app.qa.vectorstore.collection:shipflow_knowledge}")
    private String collectionName;

    @Value("${app.qa.vectorstore.dimension:384}")
    private int vectorDimension;

    @Value("${app.qa.retrieval.top-k:5}")
    private int topK;

    @Value("${app.qa.chunking.max-tokens:512}")
    private int maxTokens;

    @Value("${app.qa.chunking.overlap:50}")
    private int chunkOverlap;

    private final VectorStoreProviderFactory vectorStoreProviderFactory;

    public QAConfig(VectorStoreProviderFactory vectorStoreProviderFactory) {
        this.vectorStoreProviderFactory = vectorStoreProviderFactory;
    }

    /**
     * Creates the embedding model bean using all-MiniLM-L6-v2.
     * This model is fast, accurate, and lightweight for local inference.
     * Output dimension: 384
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        log.info("Initializing embedding model: all-MiniLM-L6-v2 (dimension: 384)");
        return new AllMiniLmL6V2EmbeddingModel();
    }

    /**
     * Creates the embedding store bean using the configured provider.
     * Uses pluggable vector store architecture - provider is selected via configuration.
     */
    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        log.info("Creating vector store with provider: {}", vectorStoreProvider);
        
        VectorStoreProviderType providerType = VectorStoreProviderType.fromConfigValue(vectorStoreProvider);
        VectorStoreProviderConfig config = buildProviderConfig(providerType);
        
        return vectorStoreProviderFactory.createStore(providerType, config);
    }

    /**
     * Builds the configuration for the selected provider.
     */
    private VectorStoreProviderConfig buildProviderConfig(VectorStoreProviderType providerType) {
        VectorStoreProviderConfig.Builder builder = VectorStoreProviderConfig.builder()
                .collectionName(collectionName)
                .dimension(vectorDimension);
        
        switch (providerType) {
            case QDRANT:
                builder.host(qdrantHost)
                       .port(qdrantPort);
                if (qdrantApiKey != null && !qdrantApiKey.trim().isEmpty()) {
                    builder.apiKey(qdrantApiKey);
                }
                break;
                
            case CHROMA:
                builder.url(chromaUrl);
                break;
                
            case IN_MEMORY:
                // No additional config needed
                break;
                
            default:
                log.warn("Using default configuration for provider: {}", providerType);
        }
        
        return builder.build();
    }

    public String getVectorStoreProvider() {
        return vectorStoreProvider;
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

    public String getCollectionName() {
        return collectionName;
    }

    public int getVectorDimension() {
        return vectorDimension;
    }
}
