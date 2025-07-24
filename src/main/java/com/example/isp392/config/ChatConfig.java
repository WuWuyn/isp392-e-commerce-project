package com.example.isp392.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * Configuration for ReadHub ChatBot with Spring AI
 * Integrates Google Gemini and FAISS Vector Store
 */
@Configuration
public class ChatConfig {

    @Value("${chat.max-history-messages:20}")
    private int maxHistoryMessages;

    @Value("${chat.session-timeout-hours:24}")
    private int sessionTimeoutHours;

    @Value("${chat.enable-rag:true}")
    private boolean enableRag;

    @Value("${chat.max-results:5}")
    private int maxResults;

    /**
     * ChatClient bean cho Spring AI - sử dụng Vertex AI Gemini
     */
    @Bean
    public ChatClient chatClient(@Qualifier("vertexAiGeminiChat") ChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * RestTemplate for HTTP requests
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    // Getters for configuration values
    public int getMaxHistoryMessages() {
        return maxHistoryMessages;
    }

    public int getSessionTimeoutHours() {
        return sessionTimeoutHours;
    }

    public boolean isEnableRag() {
        return enableRag;
    }

    public int getMaxResults() {
        return maxResults;
    }


}
