package com.example.isp392.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for EmbeddingModel with real embeddings support
 */
@Configuration
public class RealEmbeddingConfig {

    private static final Logger logger = LoggerFactory.getLogger(RealEmbeddingConfig.class);

    /**
     * Primary EmbeddingModel bean
     * Uses fallback model for now to avoid circular dependencies
     */
    @Bean
    @Primary
    public EmbeddingModel embeddingModel() {
        logger.info("Using enhanced fallback embedding model with improved semantic matching");
        return new FallbackEmbeddingModel();
    }

    /**
     * Enhanced wrapper for real embedding models with Vietnamese text optimization
     */
    public static class EnhancedEmbeddingModel implements EmbeddingModel {
        
        private final EmbeddingModel delegate;
        private static final Logger logger = LoggerFactory.getLogger(EnhancedEmbeddingModel.class);

        public EnhancedEmbeddingModel(EmbeddingModel delegate) {
            this.delegate = delegate;
        }

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(org.springframework.ai.embedding.EmbeddingRequest request) {
            try {
                // Preprocess Vietnamese text for better embeddings
                var processedRequest = preprocessRequest(request);
                return delegate.call(processedRequest);
            } catch (Exception e) {
                logger.error("Error calling embedding model: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to generate embeddings", e);
            }
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            try {
                // Preprocess Vietnamese text
                String processedContent = preprocessVietnameseText(document.getContent());
                var processedDoc = new org.springframework.ai.document.Document(processedContent, document.getMetadata());
                return delegate.embed(processedDoc);
            } catch (Exception e) {
                logger.error("Error embedding document: {}", e.getMessage(), e);
                throw new RuntimeException("Failed to embed document", e);
            }
        }

        @Override
        public int dimensions() {
            return delegate.dimensions();
        }

        /**
         * Preprocess embedding request for better Vietnamese text handling
         */
        private org.springframework.ai.embedding.EmbeddingRequest preprocessRequest(org.springframework.ai.embedding.EmbeddingRequest request) {
            var processedInstructions = request.getInstructions().stream()
                    .map(this::preprocessVietnameseText)
                    .toList();
            
            return new org.springframework.ai.embedding.EmbeddingRequest(processedInstructions, request.getOptions());
        }

        /**
         * Preprocess Vietnamese text for better embedding quality
         */
        private String preprocessVietnameseText(String text) {
            if (text == null || text.trim().isEmpty()) {
                return text;
            }

            // Normalize Vietnamese text and add context markers
            String processed = text
                    .trim()
                    .replaceAll("\\s+", " ") // Normalize whitespace
                    .toLowerCase();

            // Add language context for better embeddings
            if (containsVietnamese(processed)) {
                processed = "[Vietnamese] " + processed;
            }

            return processed;
        }

        /**
         * Check if text contains Vietnamese characters
         */
        private boolean containsVietnamese(String text) {
            return text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*");
        }
    }

    /**
     * Enhanced fallback embedding model with keyword-based semantic similarity
     */
    public static class FallbackEmbeddingModel implements EmbeddingModel {

        private static final Logger logger = LoggerFactory.getLogger(FallbackEmbeddingModel.class);

        // Common keywords for better semantic matching
        private static final String[] BOOK_KEYWORDS = {
            "title", "author", "price", "giá", "sách", "book", "rating", "đánh giá",
            "category", "thể loại", "publisher", "nhà xuất bản", "discount", "giảm giá",
            "stock", "kho", "available", "có sẵn", "description", "mô tả",
            "programming", "lập trình", "business", "kinh doanh", "psychology", "tâm lý",
            "fiction", "tiểu thuyết", "non-fiction", "phi tiểu thuyết", "education", "giáo dục",
            "self-help", "tự giúp bản thân", "science", "khoa học", "history", "lịch sử",
            "cheap", "rẻ", "expensive", "đắt", "new", "mới", "popular", "phổ biến",
            "bestseller", "bán chạy", "review", "đánh giá", "recommend", "gợi ý"
        };

        @Override
        public org.springframework.ai.embedding.EmbeddingResponse call(org.springframework.ai.embedding.EmbeddingRequest request) {
            logger.debug("Using enhanced fallback embedding model with keyword-based similarity");

            var embeddings = request.getInstructions().stream()
                    .map(instruction -> {
                        float[] vector = createKeywordBasedEmbedding(instruction);
                        return new org.springframework.ai.embedding.Embedding(vector, 0);
                    })
                    .toList();

            return new org.springframework.ai.embedding.EmbeddingResponse(embeddings);
        }

        @Override
        public float[] embed(org.springframework.ai.document.Document document) {
            return createKeywordBasedEmbedding(document.getContent());
        }

        @Override
        public int dimensions() {
            return 768; // Match text-embedding-004 dimensions
        }

        /**
         * Create keyword-based embeddings with semantic similarity
         */
        private float[] createKeywordBasedEmbedding(String content) {
            float[] vector = new float[768];
            String lowerContent = content.toLowerCase();

            // Base random component for uniqueness
            int hash = content.hashCode();
            java.util.Random random = new java.util.Random(hash);

            // Initialize with small random values
            for (int i = 0; i < 768; i++) {
                vector[i] = (float) (random.nextGaussian() * 0.05);
            }

            // Add keyword-based features for semantic similarity
            addKeywordFeatures(vector, lowerContent);

            // Add exact text matching features
            addExactMatchFeatures(vector, content, lowerContent);

            // Add price-related features
            addPriceFeatures(vector, lowerContent);

            // Add Vietnamese language features
            addVietnameseFeatures(vector, lowerContent);

            // Normalize vector
            normalizeVector(vector);

            return vector;
        }

        /**
         * Add keyword-based features to embedding vector
         */
        private void addKeywordFeatures(float[] vector, String content) {
            for (int i = 0; i < BOOK_KEYWORDS.length && i < 100; i++) {
                String keyword = BOOK_KEYWORDS[i];
                if (content.contains(keyword)) {
                    // Add feature to specific dimensions
                    int startDim = i * 7;
                    int endDim = Math.min(startDim + 7, vector.length);

                    for (int j = startDim; j < endDim; j++) {
                        vector[j] += 0.5f; // Strong signal for keyword presence
                    }
                }
            }
        }

        /**
         * Add exact text matching features for better title/author searches
         */
        private void addExactMatchFeatures(float[] vector, String originalContent, String lowerContent) {
            // Extract important phrases for exact matching
            String[] words = lowerContent.split("\\s+");

            // Title matching - look for title patterns
            if (originalContent.contains("Title:")) {
                String titleLine = extractLineContaining(originalContent, "Title:");
                if (titleLine != null) {
                    String title = titleLine.replace("Title:", "").trim().toLowerCase();
                    addTextHashFeature(vector, title, 600, 650);
                }
            }

            // Author matching
            if (originalContent.contains("Authors:")) {
                String authorLine = extractLineContaining(originalContent, "Authors:");
                if (authorLine != null) {
                    String author = authorLine.replace("Authors:", "").trim().toLowerCase();
                    addTextHashFeature(vector, author, 650, 700);
                }
            }

            // For queries, add features for each significant word
            for (String word : words) {
                if (word.length() > 2) { // Skip short words
                    addTextHashFeature(vector, word, 500, 600);
                }
            }
        }

        /**
         * Extract line containing specific text
         */
        private String extractLineContaining(String content, String searchText) {
            String[] lines = content.split("\n");
            for (String line : lines) {
                if (line.contains(searchText)) {
                    return line;
                }
            }
            return null;
        }

        /**
         * Add text-based hash feature for exact matching
         */
        private void addTextHashFeature(float[] vector, String text, int startDim, int endDim) {
            if (text == null || text.trim().isEmpty()) return;

            int hash = Math.abs(text.hashCode());
            int range = endDim - startDim;
            int targetDim = startDim + (hash % range);

            if (targetDim < vector.length) {
                vector[targetDim] += 1.0f; // Strong signal for exact text match
            }
        }

        /**
         * Add price-related features for better price queries
         */
        private void addPriceFeatures(float[] vector, String content) {
            // Price indicators
            if (content.contains("price") || content.contains("giá")) {
                addFeatureRange(vector, 700, 710, 0.8f);
            }

            // Discount indicators
            if (content.contains("discount") || content.contains("giảm giá") || content.contains("off")) {
                addFeatureRange(vector, 710, 720, 0.8f);
            }

            // Cheap/expensive indicators
            if (content.contains("cheap") || content.contains("rẻ") || content.contains("under") || content.contains("dưới")) {
                addFeatureRange(vector, 720, 730, 0.7f);
            }

            if (content.contains("expensive") || content.contains("đắt") || content.contains("premium")) {
                addFeatureRange(vector, 730, 740, 0.7f);
            }

            // Extract and categorize price ranges
            addPriceRangeFeatures(vector, content);
        }

        /**
         * Add price range features for numeric price queries
         */
        private void addPriceRangeFeatures(float[] vector, String content) {
            // Extract numbers from content
            java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("\\d+");
            java.util.regex.Matcher matcher = numberPattern.matcher(content);

            while (matcher.find()) {
                try {
                    long number = Long.parseLong(matcher.group());

                    // Categorize price ranges
                    if (number < 100000) {
                        addFeatureRange(vector, 740, 745, 0.8f); // Under 100k
                    } else if (number < 200000) {
                        addFeatureRange(vector, 745, 750, 0.8f); // 100k-200k
                    } else if (number < 300000) {
                        addFeatureRange(vector, 750, 755, 0.8f); // 200k-300k
                    } else if (number < 500000) {
                        addFeatureRange(vector, 755, 760, 0.8f); // 300k-500k
                    } else {
                        addFeatureRange(vector, 760, 765, 0.8f); // Over 500k
                    }

                } catch (NumberFormatException e) {
                    // Ignore invalid numbers
                }
            }
        }

        /**
         * Add Vietnamese language features
         */
        private void addVietnameseFeatures(float[] vector, String content) {
            if (containsVietnamese(content)) {
                addFeatureRange(vector, 740, 750, 0.6f);
            }
        }

        /**
         * Add feature values to specific dimension range
         */
        private void addFeatureRange(float[] vector, int startDim, int endDim, float strength) {
            for (int i = startDim; i < Math.min(endDim, vector.length); i++) {
                vector[i] += strength;
            }
        }

        /**
         * Check if text contains Vietnamese characters
         */
        private boolean containsVietnamese(String text) {
            return text.matches(".*[àáạảãâầấậẩẫăằắặẳẵèéẹẻẽêềếệểễìíịỉĩòóọỏõôồốộổỗơờớợởỡùúụủũưừứựửữỳýỵỷỹđ].*");
        }

        /**
         * Normalize vector to unit length
         */
        private void normalizeVector(float[] vector) {
            float norm = 0;
            for (float v : vector) {
                norm += v * v;
            }
            norm = (float) Math.sqrt(norm);

            if (norm > 0) {
                for (int i = 0; i < vector.length; i++) {
                    vector[i] /= norm;
                }
            }
        }
    }
}
