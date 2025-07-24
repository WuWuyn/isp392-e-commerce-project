package com.example.isp392.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Simple chat message entity for basic logging
 * Stores individual messages in conversations
 */
@Entity
@Table(name = "chat_messages")
@Getter
@Setter
@NoArgsConstructor
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private ChatSession chatSession;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String content;

    @Column(name = "is_user_message", nullable = false)
    private boolean isUserMessage;

    @Column(name = "message_type", length = 50)
    private String messageType = "text"; // text, book_recommendation, store_info, error

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Lob
    @Column(name = "metadata", columnDefinition = "NVARCHAR(MAX)")
    private String metadata; // JSON string for additional data (book IDs, etc.)

    public ChatMessage(ChatSession chatSession, String content, boolean isUserMessage) {
        this.chatSession = chatSession;
        this.content = content;
        this.isUserMessage = isUserMessage;
        this.createdAt = LocalDateTime.now();
    }

    public ChatMessage(ChatSession chatSession, String content, boolean isUserMessage, String messageType) {
        this(chatSession, content, isUserMessage);
        this.messageType = messageType;
    }

    public static ChatMessage createUserMessage(ChatSession session, String content) {
        return new ChatMessage(session, content, true);
    }

    public static ChatMessage createBotMessage(ChatSession session, String content) {
        return new ChatMessage(session, content, false);
    }

    public static ChatMessage createBotMessage(ChatSession session, String content, String messageType) {
        return new ChatMessage(session, content, false, messageType);
    }
}
