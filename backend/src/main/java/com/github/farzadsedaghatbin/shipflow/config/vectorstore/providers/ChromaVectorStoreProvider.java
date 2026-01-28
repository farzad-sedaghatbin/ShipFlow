package com.github.farzadsedaghatbin.shipflow.config.vectorstore.providers;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProvider;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderConfig;
import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.chroma.ChromaEmbeddingStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Vector Store Provider implementation for ChromaDB.
 * 
 * <p>ChromaDB is suitable for:
 * <ul>
 *   <li>Development and small-scale deployments</li>
 *   <li>Python-centric environments</li>
 *   <li>Simple setup requirements</li>
 * </ul>
 * 
 * <p>Configuration:
 * <ul>
 *   <li>url: ChromaDB server URL (default: http://localhost:8000)</li>
 *   <li>collectionName: Name of the collection (default: shipflow_knowledge)</li>
 * </ul>
 * 
 * <p><b>Note:</b> For production, consider using Qdrant for better performance and scalability.
 */
@Component
@Slf4j
public class ChromaVectorStoreProvider implements VectorStoreProvider {

    private static final String DEFAULT_URL = "http://localhost:8000";
    private static final int DEFAULT_PORT = 8000;

    @Override
    public VectorStoreProviderType getProviderType() {
        return VectorStoreProviderType.CHROMA;
    }

    @Override
    public EmbeddingStore<TextSegment> createStore(VectorStoreProviderConfig config) {
        validateConfig(config);
        
        String url = config.getEffectiveUrl();
        if (url == null || url.trim().isEmpty()) {
            url = DEFAULT_URL;
        }
        
        String collectionName = config.getCollectionName();
        
        log.info("Creating ChromaDB embedding store - URL: {}, Collection: {}", url, collectionName);
        
        ChromaEmbeddingStore.Builder builder = ChromaEmbeddingStore.builder()
                .baseUrl(url)
                .collectionName(collectionName);
        
        // Handle timeout if specified
        Integer timeoutSeconds = config.getExtraParam("timeoutSeconds", null);
        if (timeoutSeconds != null) {
            builder.timeout(java.time.Duration.ofSeconds(timeoutSeconds));
        }
        
        // Handle log requests for debugging
        Boolean logRequests = config.getExtraParam("logRequests", false);
        if (logRequests) {
            builder.logRequests(true);
            builder.logResponses(true);
        }
        
        return builder.build();
    }

    @Override
    public void validateConfig(VectorStoreProviderConfig config) {
        if (config.getCollectionName() == null || config.getCollectionName().trim().isEmpty()) {
            throw new IllegalArgumentException("ChromaDB collection name is required");
        }
        
        log.debug("Validated ChromaDB configuration: {}", config);
    }

    @Override
    public boolean requiresApiKey() {
        return false; // ChromaDB basic auth is optional
    }

    @Override
    public boolean requiresUrl() {
        return true;
    }

    @Override
    public int getDefaultPort() {
        return DEFAULT_PORT;
    }

    @Override
    public String getDescription() {
        return "ChromaDB - Simple vector store for development and small-scale deployments";
    }
}
