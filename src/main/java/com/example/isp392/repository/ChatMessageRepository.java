package com.example.isp392.repository;

import com.example.isp392.model.ChatMessage;
import com.example.isp392.model.ChatSession;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for ChatMessage entity
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find recent messages for a session
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession = :session ORDER BY cm.createdAt DESC")
    List<ChatMessage> findRecentMessagesBySession(@Param("session") ChatSession session, Pageable pageable);

    /**
     * Find messages by session ordered by creation time
     */
    List<ChatMessage> findByChatSessionOrderByCreatedAtAsc(ChatSession session);

    /**
     * Count messages in a session
     */
    long countByChatSession(ChatSession session);

    /**
     * Find messages created after a certain time
     */
    @Query("SELECT cm FROM ChatMessage cm WHERE cm.chatSession = :session AND cm.createdAt > :afterTime ORDER BY cm.createdAt ASC")
    List<ChatMessage> findMessagesBySessionAfterTime(@Param("session") ChatSession session, @Param("afterTime") LocalDateTime afterTime);

    /**
     * Delete old messages (for cleanup)
     */
    @Query("DELETE FROM ChatMessage cm WHERE cm.createdAt < :cutoffTime")
    int deleteOldMessages(@Param("cutoffTime") LocalDateTime cutoffTime);
}
