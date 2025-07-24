package com.example.isp392.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google AI Client - Direct HTTP calls to Google AI API
 * Bypasses Vertex AI to use API key directly
 */
@Service
public class GoogleAIClient {

    private static final Logger logger = LoggerFactory.getLogger(GoogleAIClient.class);
    private static final String GOOGLE_AI_API_URL = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${spring.ai.vertex.ai.gemini.api-key:YOUR_API_KEY_HERE}")
    private String apiKey;

    @Value("${spring.ai.vertex.ai.gemini.chat.options.model:gemini-1.5-flash}")
    private String modelName;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleAIClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate content using Google AI API
     */
    public String generateContent(String prompt) {
        if (!isConfigured()) {
            throw new IllegalStateException("Google AI API key not configured");
        }

        try {
            String url = GOOGLE_AI_API_URL + modelName + ":generateContent?key=" + apiKey;
            
            // Create request body
            Map<String, Object> requestBody = new HashMap<>();
            Map<String, Object> content = new HashMap<>();
            Map<String, String> part = new HashMap<>();
            part.put("text", prompt);
            content.put("parts", List.of(part));
            requestBody.put("contents", List.of(content));

            // Create headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // Create request entity
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            // Make API call
            logger.debug("Calling Google AI API with model: {}", modelName);
            ResponseEntity<String> response = restTemplate.exchange(
                url, 
                HttpMethod.POST, 
                requestEntity, 
                String.class
            );

            // Parse response
            return extractTextFromResponse(response.getBody());

        } catch (Exception e) {
            logger.error("Error calling Google AI API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate content: " + e.getMessage(), e);
        }
    }

    /**
     * Extract text from Google AI API response
     */
    private String extractTextFromResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                
                if (parts.isArray() && parts.size() > 0) {
                    String text = parts.get(0).path("text").asText();
                    if (!text.isEmpty()) {
                        logger.debug("Generated response length: {} characters", text.length());
                        return text;
                    }
                }
            }
            
            logger.warn("No text found in response: {}", responseJson);
            return "Xin lỗi, tôi không thể tạo phản hồi lúc này.";
            
        } catch (Exception e) {
            logger.error("Error parsing response: {}", e.getMessage());
            return "Xin lỗi, có lỗi khi xử lý phản hồi.";
        }
    }

    /**
     * Check if Google AI is configured
     */
    public boolean isConfigured() {
        return !"YOUR_API_KEY_HERE".equals(apiKey) && 
               apiKey != null && 
               !apiKey.trim().isEmpty() &&
               apiKey.startsWith("AIza");
    }

    /**
     * Get API key status for logging
     */
    public String getApiKeyStatus() {
        if (!isConfigured()) {
            return "Not configured";
        }
        return apiKey.substring(0, 8) + "***";
    }
}
