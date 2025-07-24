package com.example.isp392.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration for Vector Store
 */
@Configuration
public class VectorStoreConfig {

    private static final Logger logger = LoggerFactory.getLogger(VectorStoreConfig.class);

    @Autowired
    private EmbeddingModel embeddingModel;

    /**
     * Create SimpleVectorStore with file persistence
     */
    @Bean
    public VectorStore vectorStore() {
        try {
            logger.info("Initializing Simple Vector Store...");
            
            // Create data directory if it doesn't exist
            Path dataDir = Paths.get("data");
            if (!dataDir.toFile().exists()) {
                dataDir.toFile().mkdirs();
                logger.info("Created data directory: {}", dataDir.toAbsolutePath());
            }
            
            // Vector store file path
            File vectorStoreFile = new File("data/vector-store.json");
            logger.info("Vector store path: {}", vectorStoreFile.getAbsolutePath());
            
            // Create SimpleVectorStore
            SimpleVectorStore vectorStore = new SimpleVectorStore(embeddingModel);
            
            // Load existing data if file exists
            if (vectorStoreFile.exists()) {
                try {
                    vectorStore.load(vectorStoreFile);
                    logger.info("Loaded existing vector store from: {}", vectorStoreFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.warn("Could not load existing vector store, starting fresh: {}", e.getMessage());
                }
            }
            
            // Save on shutdown
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    vectorStore.save(vectorStoreFile);
                    logger.info("Vector store saved to: {}", vectorStoreFile.getAbsolutePath());
                } catch (Exception e) {
                    logger.error("Error saving vector store: {}", e.getMessage());
                }
            }));
            
            logger.info("Simple Vector Store initialized successfully");
            return vectorStore;
            
        } catch (Exception e) {
            logger.error("Failed to initialize Vector Store: {}", e.getMessage(), e);
            throw new RuntimeException("Vector Store initialization failed", e);
        }
    }
}
