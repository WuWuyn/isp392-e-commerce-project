package com.example.isp392.service;

import com.example.isp392.config.ChatConfig;
import com.example.isp392.model.ChatMessage;
import com.example.isp392.model.ChatSession;
import com.example.isp392.model.User;
import com.example.isp392.repository.ChatMessageRepository;
import com.example.isp392.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ReadHub Chat Service - Smart chat service with Google AI and FAISS Vector Store
 * Uses RAG (Retrieval-Augmented Generation) for accurate responses
 */
@Service
@Transactional
public class ChatService {

    private static final Logger logger = LoggerFactory.getLogger(ChatService.class);

    private final ChatConfig chatConfig;
    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final VectorStore vectorStore;
    private final GoogleAIClient googleAIClient;
    private final EmbeddingModel embeddingModel;

    @Autowired
    public ChatService(ChatConfig chatConfig,
                      ChatSessionRepository chatSessionRepository,
                      ChatMessageRepository chatMessageRepository,
                      @Autowired(required = false) VectorStore vectorStore,
                      GoogleAIClient googleAIClient,
                      EmbeddingModel embeddingModel) {
        this.chatConfig = chatConfig;
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.vectorStore = vectorStore;
        this.googleAIClient = googleAIClient;
        this.embeddingModel = embeddingModel;
    }

    // In-memory conversation history cache for better context preservation
    private final Map<Long, ConversationContext> conversationCache = new ConcurrentHashMap<>();

    /**
     * Process a user message and return a response
     */
    public String processMessage(String userMessage, String sessionToken, User user) {
        try {
            if (user == null) {
                throw new IllegalArgumentException("User must be authenticated to use chat");
            }

            ChatSession session = getOrCreateSession(sessionToken, user);

            // Save user message
            ChatMessage userChatMessage = new ChatMessage(session, userMessage, true);
            chatMessageRepository.save(userChatMessage);

            // Generate response using RAG with enhanced context
            String rawResponse = generateResponseWithRAG(userMessage, session);

            // Clean Markdown formatting from response
            String response = cleanMarkdownFromResponse(rawResponse);

            // Save bot response
            ChatMessage botMessage = new ChatMessage(session, response, false);
            chatMessageRepository.save(botMessage);

            // Update conversation context cache
            updateConversationContext(user.getUserId().longValue(), userMessage, response);

            // Update session timestamp
            session.setUpdatedAt(LocalDateTime.now());
            chatSessionRepository.save(session);

            return response;

        } catch (Exception e) {
            logger.error("Error processing chat message: {}", e.getMessage(), e);
            return "Sorry, I encountered an error processing your message. Please try again.";
        }
    }

    /**
     * Generate response using RAG (Retrieval-Augmented Generation)
     */
    private String generateResponseWithRAG(String userMessage, ChatSession session) {
        try {
            if (!googleAIClient.isConfigured()) {
                return generateFallbackResponse(userMessage);
            }

            String context = getContextFromVectorStore(userMessage);
            String conversationHistory = getRecentConversationContext(session);
            String systemPrompt = createSystemPrompt(context, conversationHistory);
            String fullPrompt = systemPrompt + "\n\nUser: " + userMessage + "\nAssistant:";

            return googleAIClient.generateContent(fullPrompt);

        } catch (Exception e) {
            logger.error("Error generating RAG response: {}", e.getMessage());
            return generateFallbackResponse(userMessage);
        }
    }

    /**
     * Get relevant context from Vector Store
     */
    private String getContextFromVectorStore(String userMessage) {
        if (vectorStore == null) {
            return getBasicContext();
        }

        try {
            // Use optimized thresholds based on embedding model capabilities
            double threshold = isUsingRealEmbeddings() ? 0.3 : 0.1; // Lower threshold for enhanced fallback model

            SearchRequest searchRequest = SearchRequest.query(userMessage)
                    .withTopK(chatConfig.getMaxResults())
                    .withSimilarityThreshold(threshold);

            List<Document> relevantDocs = vectorStore.similaritySearch(searchRequest);

            // Log search results for debugging
            logger.debug("Vector search for '{}' returned {} documents with threshold {}",
                        userMessage, relevantDocs.size(), threshold);

            return buildContext(relevantDocs);

        } catch (Exception e) {
            logger.warn("Error searching vector store: {}", e.getMessage());
            return getBasicContext();
        }
    }





    /**
     * Get basic ReadHub context when vector store is not available
     */
    private String getBasicContext() {
        return """
                READHUB INFORMATION:
                - ReadHub is Vietnam's leading online bookstore
                - Specializes in diverse books: literature, science, technology, business, children's books, academic
                - Has buyer (customer) and seller systems
                - Supports nationwide delivery, diverse payment methods
                - Flexible return policy within 7 days
                - Regular promotional programs
                - Website: http://localhost:8080

                Please answer based on this information and general book knowledge.
                """;
    }

    /**
     * Build context from retrieved documents with enhanced formatting
     */
    private String buildContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return getBasicContext();
        }

        StringBuilder context = new StringBuilder();
        context.append("AVAILABLE BOOKS FROM READHUB:\n\n");

        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);

            // Extract comprehensive marketplace information from metadata
            Map<String, Object> metadata = doc.getMetadata();
            String title = (String) metadata.get("title");
            String price = (String) metadata.get("sellingPrice");
            String originalPrice = (String) metadata.get("originalPrice");
            String stock = (String) metadata.get("stockQuantity");
            Boolean hasDiscount = (Boolean) metadata.get("hasDiscount");

            // Marketplace-specific metadata
            String shopName = (String) metadata.get("shopName");
            String shopRating = (String) metadata.get("shopRating");
            Boolean isVerifiedSeller = (Boolean) metadata.get("isVerifiedSeller");
            Boolean hasActivePromotions = (Boolean) metadata.get("hasActivePromotions");
            String bestPromotionCode = (String) metadata.get("bestPromotionCode");
            String availablePromotions = (String) metadata.get("availablePromotions");
            Boolean freeShipping = (Boolean) metadata.get("freeShipping");

            context.append("BOOK ").append(i + 1).append(":\n");

            // Enhanced summary line with marketplace info
            if (title != null && price != null) {
                context.append("📚 ").append(title);
                context.append(" - ").append(price).append(" VND");

                if (hasDiscount != null && hasDiscount && originalPrice != null) {
                    context.append(" (was ").append(originalPrice).append(" VND)");
                }

                if (stock != null) {
                    context.append(" - ").append(stock).append(" in stock");
                }

                // Add seller info
                if (shopName != null) {
                    context.append(" - Sold by: ").append(shopName);
                    if (isVerifiedSeller != null && isVerifiedSeller) {
                        context.append(" ✓");
                    }
                    if (shopRating != null) {
                        context.append(" (").append(shopRating).append("⭐)");
                    }
                }

                context.append("\n");

                // Add promotion info in summary
                if (hasActivePromotions != null && hasActivePromotions && bestPromotionCode != null) {
                    context.append("🎫 Promotion: ").append(bestPromotionCode);
                    if (availablePromotions != null) {
                        context.append(" (+ more available)");
                    }
                    context.append("\n");
                }

                // Add shipping info
                if (freeShipping != null && freeShipping) {
                    context.append("🚚 Free shipping included\n");
                }
            }

            // Add full details
            context.append(doc.getContent());
            context.append("\n" + "=".repeat(50) + "\n\n");
        }

        return context.toString();
    }

    /**
     * Create flexible system prompt for natural marketplace conversations
     */
    private String createSystemPrompt(String context, String conversationHistory) {
        return String.format("""
            You are ReadHub Assistant, a helpful AI assistant for ReadHub - Vietnam's multi-vendor book marketplace.

            CORE INSTRUCTIONS:
            - Use the marketplace information provided below to answer questions accurately
            - Provide specific book details, seller information, and purchasing options when available
            - Answer in Vietnamese naturally and conversationally
            - Be helpful and informative while maintaining a friendly tone
            - IMPORTANT: Return responses as plain text only. Do NOT use any Markdown formatting (**, *, #, etc.)

            AVAILABLE MARKETPLACE DATA:
            %s

            RECENT CONVERSATION:
            %s

            GUIDELINES:
            • When recommending books, include relevant details like title, author, price, and seller information
            • Mention promotions, discounts, and special offers when applicable
            • Provide payment and shipping information when relevant to the query
            • Compare options when multiple choices are available
            • Highlight verified sellers and trust indicators naturally
            • Adapt your response style to match the customer's question - simple queries get simple answers, complex queries get detailed responses
            • Use plain text formatting only - no bold, italic, headers, or other markup

            Respond naturally and helpfully to the customer's question, using the provided information to give accurate and useful advice.
            """, context, conversationHistory);
    }

    /**
     * Generate fallback response when AI is not available
     */
    private String generateFallbackResponse(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();

        if (lowerMessage.contains("xin chào") || lowerMessage.contains("hello") || lowerMessage.contains("hi")) {
            return "Xin chào! Tôi là ReadHub Assistant. Tôi có thể giúp bạn tìm sách, tư vấn về chính sách cửa hàng. Bạn cần hỗ trợ gì?";
        } else if (lowerMessage.contains("sách") || lowerMessage.contains("book")) {
            return "Tôi có thể giúp bạn tìm sách theo thể loại, tác giả hoặc mức giá. Bạn đang tìm loại sách nào?";
        } else if (lowerMessage.contains("giá") || lowerMessage.contains("price")) {
            return "Tôi có thể tư vấn về giá sách và các chương trình khuyến mãi. Bạn quan tâm đến sách nào?";
        } else if (lowerMessage.contains("chính sách") || lowerMessage.contains("policy")) {
            return "ReadHub có chính sách đổi trả trong 7 ngày, miễn phí vận chuyển từ 500k, và nhiều phương thức thanh toán. Bạn cần biết thêm về điều gì?";
        } else {
            return "Tôi là ReadHub Assistant, có thể giúp bạn:\n" +
                   "📚 Tìm sách theo sở thích\n" +
                   "💰 Tư vấn giá và khuyến mãi\n" +
                   "🏪 Thông tin chính sách cửa hàng\n" +
                   "Bạn cần hỗ trợ gì?";
        }
    }





    /**
     * Get or create a chat session
     */
    private ChatSession getOrCreateSession(String sessionToken, User user) {
        // Try to find existing active session
        Optional<ChatSession> existingSession = chatSessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken);
        
        if (existingSession.isPresent()) {
            ChatSession session = existingSession.get();
            // Check if session is not too old
            LocalDateTime cutoff = LocalDateTime.now().minusHours(chatConfig.getSessionTimeoutHours());
            if (session.getUpdatedAt().isAfter(cutoff)) {
                return session;
            }
        }

        // Create new session
        ChatSession newSession = new ChatSession(sessionToken, user);
        return chatSessionRepository.save(newSession);
    }

    /**
     * Get recent conversation context for better responses
     */
    private String getRecentConversationContext(ChatSession session) {
        try {
            Pageable pageable = PageRequest.of(0, 6); // Last 6 messages (3 exchanges)
            List<ChatMessage> recentMessages = chatMessageRepository.findRecentMessagesBySession(session, pageable);
            
            if (recentMessages.isEmpty()) {
                return "This is the start of a new conversation.";
            }

            StringBuilder context = new StringBuilder();
            for (int i = recentMessages.size() - 1; i >= 0; i--) { // Reverse to chronological order
                ChatMessage msg = recentMessages.get(i);
                String sender = msg.isUserMessage() ? "User" : "Assistant";
                context.append(sender).append(": ").append(msg.getContent()).append("\n");
            }
            
            return context.toString();

        } catch (Exception e) {
            logger.error("Error getting conversation context: {}", e.getMessage());
            return "Previous conversation context unavailable.";
        }
    }

    /**
     * Generate a unique session token
     */
    public String generateSessionToken() {
        return "session_" + UUID.randomUUID().toString();
    }

    /**
     * Get chat history for a session
     */
    public List<ChatMessage> getChatHistory(String sessionToken) {
        try {
            Optional<ChatSession> session = chatSessionRepository.findBySessionTokenAndIsActiveTrue(sessionToken);
            if (session.isPresent()) {
                return chatMessageRepository.findByChatSessionOrderByCreatedAtAsc(session.get());
            }
        } catch (Exception e) {
            logger.error("Error getting chat history: {}", e.getMessage());
        }
        return List.of();
    }

    /**
     * Get recent conversation sessions for a user (for conversation history sidebar)
     */
    public List<ChatSession> getRecentConversations(User user, int limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit);
            return chatSessionRepository.findRecentConversationsByUser(user, pageable);
        } catch (Exception e) {
            logger.error("Error retrieving recent conversations for user {}: {}", user.getUserId(), e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Create a new chat session for a user
     */
    public ChatSession createNewSession(User user) {
        try {
            String newSessionToken = generateSessionToken();
            ChatSession newSession = new ChatSession(newSessionToken, user);
            return chatSessionRepository.save(newSession);
        } catch (Exception e) {
            logger.error("Error creating new session: {}", e.getMessage());
            throw new RuntimeException("Failed to create new chat session");
        }
    }

    /**
     * Switch to an existing session (if it belongs to the user)
     */
    public ChatSession switchToSession(String sessionToken, User user) {
        try {
            Optional<ChatSession> session = chatSessionRepository.findBySessionToken(sessionToken);
            if (session.isPresent() && session.get().getUser().getUserId().equals(user.getUserId())) {
                return session.get();
            }
            throw new RuntimeException("Session not found or access denied");
        } catch (Exception e) {
            logger.error("Error switching to session: {}", e.getMessage());
            throw new RuntimeException("Failed to switch to session");
        }
    }

    /**
     * Get all active sessions for a user
     */
    public List<ChatSession> getAllUserSessions(User user) {
        try {
            return chatSessionRepository.findAllActiveSessionsByUser(user);
        } catch (Exception e) {
            logger.error("Error getting user sessions: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Switch to a different conversation session
     */
    public Map<String, Object> switchToConversation(String sessionToken, User user) {
        Map<String, Object> result = new HashMap<>();

        try {
            Optional<ChatSession> sessionOpt = chatSessionRepository.findBySessionToken(sessionToken);

            if (sessionOpt.isEmpty()) {
                result.put("success", false);
                result.put("message", "Conversation not found");
                return result;
            }

            ChatSession session = sessionOpt.get();

            // Verify user owns this session
            if (!session.getUser().getUserId().equals(user.getUserId())) {
                result.put("success", false);
                result.put("message", "Access denied");
                return result;
            }

            // Get messages for this session
            List<ChatMessage> messages = chatMessageRepository.findByChatSessionOrderByCreatedAtAsc(session);

            result.put("success", true);
            result.put("sessionToken", sessionToken);
            result.put("title", session.getDisplayTitle());
            result.put("messages", messages);
            result.put("messageCount", messages.size());

            return result;

        } catch (Exception e) {
            logger.error("Error switching to conversation {}: {}", sessionToken, e.getMessage());
            result.put("success", false);
            result.put("message", "Error loading conversation");
            return result;
        }
    }

    /**
     * Create a new conversation session
     */
    public String createNewConversation(User user) {
        try {
            String sessionToken = generateSessionToken();
            ChatSession session = new ChatSession(sessionToken, user);
            chatSessionRepository.save(session);

            logger.info("Created new conversation session {} for user {}", sessionToken, user.getUserId());
            return sessionToken;

        } catch (Exception e) {
            logger.error("Error creating new conversation for user {}: {}", user.getUserId(), e.getMessage());
            return generateSessionToken(); // Fallback
        }
    }

    /**
     * Clean response text by removing Markdown formatting
     */
    private String cleanMarkdownFromResponse(String response) {
        if (response == null) {
            return "";
        }

        return response
                // Remove bold formatting
                .replaceAll("\\*\\*(.*?)\\*\\*", "$1")
                .replaceAll("__(.*?)__", "$1")
                // Remove italic formatting
                .replaceAll("\\*(.*?)\\*", "$1")
                .replaceAll("_(.*?)_", "$1")
                // Remove headers
                .replaceAll("^#{1,6}\\s+", "")
                // Remove code blocks
                .replaceAll("```[\\s\\S]*?```", "")
                .replaceAll("`([^`]+)`", "$1")
                // Remove links but keep text
                .replaceAll("\\[([^\\]]+)\\]\\([^\\)]+\\)", "$1")
                // Remove list markers
                .replaceAll("^[\\s]*[-*+]\\s+", "")
                .replaceAll("^[\\s]*\\d+\\.\\s+", "")
                // Clean up extra whitespace
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    /**
     * Update conversation context after new message
     */
    private void updateConversationContext(Long userId, String userMessage, String botResponse) {
        ConversationContext context = conversationCache.get(userId);
        if (context == null) {
            context = new ConversationContext("");
        }

        context.addMessage(userMessage, botResponse);
        conversationCache.put(userId, context);
    }

    /**
     * Check if using real embeddings (not fallback)
     */
    private boolean isUsingRealEmbeddings() {
        return embeddingModel != null &&
               embeddingModel.getClass().getSimpleName().contains("Enhanced");
    }

    /**
     * Inner class for managing conversation context
     */
    private static class ConversationContext {
        private StringBuilder history;
        private long lastUpdated;
        private static final long EXPIRY_TIME = 30 * 60 * 1000; // 30 minutes

        public ConversationContext(String initialHistory) {
            this.history = new StringBuilder(initialHistory);
            this.lastUpdated = System.currentTimeMillis();
        }

        public void addMessage(String userMessage, String botResponse) {
            history.append("User: ").append(userMessage).append("\n");
            history.append("Assistant: ").append(botResponse).append("\n");
            this.lastUpdated = System.currentTimeMillis();
        }

        public String getFormattedHistory() {
            return history.toString();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - lastUpdated > EXPIRY_TIME;
        }
    }
}
