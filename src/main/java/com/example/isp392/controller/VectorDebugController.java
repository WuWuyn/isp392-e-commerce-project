package com.example.isp392.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Debug controller for testing vector search functionality
 */
@RestController
@RequestMapping("/api/debug")
public class VectorDebugController {

    private static final Logger logger = LoggerFactory.getLogger(VectorDebugController.class);

    @Autowired(required = false)
    private VectorStore vectorStore;

    /**
     * Test vector search with different queries and thresholds
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> testVectorSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int topK,
            @RequestParam(defaultValue = "0.3") double threshold) {
        
        Map<String, Object> response = new HashMap<>();
        
        if (vectorStore == null) {
            response.put("error", "Vector Store not available");
            return ResponseEntity.ok(response);
        }

        try {
            logger.info("Testing vector search with query: '{}', topK: {}, threshold: {}", 
                       query, topK, threshold);

            SearchRequest searchRequest = SearchRequest.query(query)
                    .withTopK(topK)
                    .withSimilarityThreshold(threshold);

            List<Document> results = vectorStore.similaritySearch(searchRequest);
            
            response.put("query", query);
            response.put("topK", topK);
            response.put("threshold", threshold);
            response.put("resultsCount", results.size());
            
            // Add detailed results
            for (int i = 0; i < results.size(); i++) {
                Document doc = results.get(i);
                Map<String, Object> docInfo = new HashMap<>();
                docInfo.put("id", doc.getId());
                docInfo.put("content", doc.getContent());
                docInfo.put("metadata", doc.getMetadata());
                response.put("result_" + (i + 1), docInfo);
            }

            logger.info("Vector search returned {} results", results.size());
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error in vector search test: {}", e.getMessage(), e);
            response.put("error", "Search failed: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Test specific book price queries
     */
    @GetMapping("/test-price-queries")
    public ResponseEntity<Map<String, Object>> testPriceQueries() {
        Map<String, Object> response = new HashMap<>();
        
        if (vectorStore == null) {
            response.put("error", "Vector Store not available");
            return ResponseEntity.ok(response);
        }

        String[] testQueries = {
            "giá sách",
            "sách dưới 200000",
            "sách giảm giá",
            "price book",
            "cheap books",
            "books under 200k"
        };

        for (String query : testQueries) {
            try {
                SearchRequest searchRequest = SearchRequest.query(query)
                        .withTopK(3)
                        .withSimilarityThreshold(0.3);

                List<Document> results = vectorStore.similaritySearch(searchRequest);
                
                Map<String, Object> queryResult = new HashMap<>();
                queryResult.put("resultsCount", results.size());
                queryResult.put("hasResults", !results.isEmpty());
                
                if (!results.isEmpty()) {
                    queryResult.put("firstResult", results.get(0).getContent().substring(0, 
                        Math.min(200, results.get(0).getContent().length())));
                }
                
                response.put(query, queryResult);
                
            } catch (Exception e) {
                Map<String, Object> errorResult = new HashMap<>();
                errorResult.put("error", e.getMessage());
                response.put(query, errorResult);
            }
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get vector store statistics
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getVectorStoreStats() {
        Map<String, Object> response = new HashMap<>();
        
        if (vectorStore == null) {
            response.put("error", "Vector Store not available");
            return ResponseEntity.ok(response);
        }

        try {
            // Test with a broad query to get all documents
            SearchRequest searchRequest = SearchRequest.query("book")
                    .withTopK(100)
                    .withSimilarityThreshold(0.0);

            List<Document> allDocs = vectorStore.similaritySearch(searchRequest);
            
            response.put("totalDocuments", allDocs.size());
            response.put("vectorStoreType", vectorStore.getClass().getSimpleName());
            
            // Sample document info
            if (!allDocs.isEmpty()) {
                Document sample = allDocs.get(0);
                response.put("sampleDocumentId", sample.getId());
                response.put("sampleContentLength", sample.getContent().length());
                response.put("sampleMetadataKeys", sample.getMetadata().keySet());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error getting vector store stats: {}", e.getMessage(), e);
            response.put("error", "Failed to get stats: " + e.getMessage());
            return ResponseEntity.ok(response);
        }
    }
}
