package com.example.isp392.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

/**
 * Helper class to validate and display Google AI configuration
 * Provides clear guidance for setup
 */
@Component
public class GoogleCloudConfigHelper {

    private static final Logger logger = LoggerFactory.getLogger(GoogleCloudConfigHelper.class);

    @Value("${spring.ai.vertex.ai.gemini.api-key:YOUR_API_KEY_HERE}")
    private String apiKey;

    @PostConstruct
    public void validateConfiguration() {
        logger.info("=== Google AI Configuration Check ===");

        if ("YOUR_API_KEY_HERE".equals(apiKey)) {
            logger.warn("⚠️  Google AI API key is not configured!");
            logger.warn("📝 To enable AI features, please:");
            logger.warn("   1. Get your free API key at: https://aistudio.google.com/app/apikey");
            logger.warn("   2. Update application.properties:");
            logger.warn("      spring.ai.google.gemini.api-key=AIzaSyC...");
            logger.warn("💡 The chatbot will work with basic responses without AI features");
        } else if (apiKey.startsWith("AIza")) {
            logger.info("✅ Google AI API key configured: {}***", apiKey.substring(0, 8));
            logger.info("🤖 Using Google AI Gemini models");
            logger.info("💡 Free tier: 15 requests per minute, 1500 requests per day");
        } else {
            logger.warn("⚠️  API key format may be incorrect. Should start with 'AIza'");
        }

        logger.info("🔗 Get API key: https://aistudio.google.com/app/apikey");
        logger.info("=====================================");
    }

    public boolean isConfigured() {
        return !"YOUR_API_KEY_HERE".equals(apiKey) && apiKey.startsWith("AIza");
    }

    public String getApiKey() {
        return apiKey;
    }
}
