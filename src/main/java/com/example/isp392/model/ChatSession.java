package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Simple chat session entity for basic logging
 * Tracks conversation sessions (can be anonymous or authenticated)
 */
@Entity
@Table(name = "chat_sessions")
@Getter
@Setter
@NoArgsConstructor
public class ChatSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "session_token", unique = true, nullable = false)
    private String sessionToken;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user; // Required - only authenticated users can use chat

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "title", length = 255)
    private String title; // Derived from first user message

    @Column(name = "message_count", nullable = false)
    private Integer messageCount = 0;

    @OneToMany(mappedBy = "chatSession", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnore
    private List<ChatMessage> messages = new ArrayList<>();

    public ChatSession(String sessionToken, User user) {
        this.sessionToken = sessionToken;
        this.user = user;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.isActive = true;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addMessage(ChatMessage message) {
        messages.add(message);
        message.setChatSession(this);
        this.messageCount++;
        this.updatedAt = LocalDateTime.now();

        // Set title from first user message if not already set
        if (this.title == null && message.isUserMessage() && message.getContent() != null) {
            this.title = generateTitle(message.getContent());
        }
    }

    /**
     * Generate a conversation title from the first user message
     */
    private String generateTitle(String firstMessage) {
        if (firstMessage == null || firstMessage.trim().isEmpty()) {
            return "New Conversation";
        }

        String cleaned = firstMessage.trim();
        if (cleaned.length() <= 40) {
            return cleaned;
        }

        // Find a good break point (space, punctuation)
        int breakPoint = 37;
        for (int i = 37; i >= 20; i--) {
            char c = cleaned.charAt(i);
            if (c == ' ' || c == ',' || c == '.' || c == '?' || c == '!') {
                breakPoint = i;
                break;
            }
        }

        return cleaned.substring(0, breakPoint) + "...";
    }

    /**
     * Get display title for the conversation
     */
    public String getDisplayTitle() {
        return title != null ? title : "New Conversation";
    }
}
