package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.config.vectorstore.VectorStoreProviderType;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import io.qdrant.client.grpc.Collections;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;

/**
 * Initializes the vector store on application startup.
 * 
 * <p>For Qdrant, ensures the collection exists with proper configuration.
 * This is required because Qdrant doesn't auto-create collections.
 * 
 * <p>Runs early in the startup sequence (Order 0) to ensure the collection
 * is ready before any knowledge ingestion occurs.
 */
@Component
@Slf4j
@Order(0) // Run before all other CommandLineRunners
@ConditionalOnProperty(name = "app.features.qa.enabled", havingValue = "true")
public class VectorStoreInitializer implements CommandLineRunner {

    @Value("${app.qa.vectorstore.provider:in-memory}")
    private String vectorStoreProvider;

    @Value("${app.qa.vectorstore.qdrant.host:localhost}")
    private String qdrantHost;

    @Value("${app.qa.vectorstore.qdrant.port:6334}")
    private int qdrantPort;

    @Value("${app.qa.vectorstore.qdrant.api-key:}")
    private String qdrantApiKey;

    @Value("${app.qa.vectorstore.collection:shipflow_knowledge}")
    private String collectionName;

    @Value("${app.qa.vectorstore.dimension:384}")
    private int vectorDimension;

    @Override
    public void run(String... args) {
        VectorStoreProviderType providerType = VectorStoreProviderType.fromConfigValue(vectorStoreProvider);
        
        log.info("Initializing vector store - Provider: {}, Collection: {}", providerType, collectionName);
        
        if (providerType == VectorStoreProviderType.QDRANT) {
            initializeQdrantCollection();
        } else {
            log.info("Vector store provider '{}' does not require initialization", providerType);
        }
    }

    /**
     * Initializes the Qdrant collection if it doesn't exist.
     */
    private void initializeQdrantCollection() {
        QdrantClient client = null;
        try {
            log.info("Connecting to Qdrant at {}:{}", qdrantHost, qdrantPort);
            
            // Build Qdrant client
            QdrantGrpcClient.Builder grpcBuilder = QdrantGrpcClient.newBuilder(qdrantHost, qdrantPort, false);
            
            if (qdrantApiKey != null && !qdrantApiKey.trim().isEmpty()) {
                grpcBuilder.withApiKey(qdrantApiKey);
                log.info("Qdrant API key configured");
            }
            
            client = new QdrantClient(grpcBuilder.build());
            
            // Check if collection exists
            boolean collectionExists = false;
            try {
                client.getCollectionInfoAsync(collectionName).get();
                collectionExists = true;
                log.info("Qdrant collection '{}' already exists", collectionName);
            } catch (ExecutionException e) {
                if (e.getCause() != null && e.getCause().getMessage().contains("doesn't exist")) {
                    log.info("Qdrant collection '{}' does not exist - will create", collectionName);
                } else {
                    throw e;
                }
            }
            
            // Create collection if it doesn't exist
            if (!collectionExists) {
                log.info("Creating Qdrant collection '{}' with dimension {}", collectionName, vectorDimension);
                
                Collections.VectorParams vectorParams = Collections.VectorParams.newBuilder()
                        .setSize(vectorDimension)
                        .setDistance(Collections.Distance.Cosine)
                        .build();
                
                client.createCollectionAsync(
                        collectionName,
                        Collections.VectorsConfig.newBuilder()
                                .setParams(vectorParams)
                                .build()
                ).get();
                
                log.info("Successfully created Qdrant collection '{}'", collectionName);
            }
            
            // Verify collection info
            Collections.CollectionInfo collectionInfo = client.getCollectionInfoAsync(collectionName).get();
            log.info("Qdrant collection '{}' ready - Status: {}, Vectors count: {}", 
                    collectionName, 
                    collectionInfo.getStatus(),
                    collectionInfo.getVectorsCount());
            
        } catch (Exception e) {
            log.error("Failed to initialize Qdrant collection '{}': {}", collectionName, e.getMessage());
            log.error("Qdrant vector store will not be available. Stack trace:", e);
            log.warn("Knowledge base features (document Q&A) will be degraded");
            log.warn("Please ensure Qdrant is running at {}:{} or switch to in-memory provider", qdrantHost, qdrantPort);
        } finally {
            if (client != null) {
                try {
                    client.close();
                } catch (Exception e) {
                    log.warn("Error closing Qdrant client: {}", e.getMessage());
                }
            }
        }
    }
}
