package com.example.isp392.repository;

import com.example.isp392.model.ChatSession;
import com.example.isp392.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for ChatSession entity
 */
@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {

    /**
     * Find active session by token
     */
    Optional<ChatSession> findBySessionTokenAndIsActiveTrue(String sessionToken);

    /**
     * Find most recent active session for a user
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user AND cs.isActive = true ORDER BY cs.updatedAt DESC")
    List<ChatSession> findMostRecentActiveSessionByUser(@Param("user") User user, Pageable pageable);

    /**
     * Find most recent active session for anonymous users (by token pattern)
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user IS NULL AND cs.sessionToken = :token AND cs.isActive = true ORDER BY cs.updatedAt DESC")
    List<ChatSession> findMostRecentAnonymousSession(@Param("token") String token, Pageable pageable);

    /**
     * Find sessions that haven't been updated recently (for cleanup)
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.updatedAt < :cutoffTime AND cs.isActive = true")
    List<ChatSession> findStaleActiveSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Count active sessions for a user
     */
    long countByUserAndIsActiveTrue(User user);

    /**
     * Deactivate old sessions
     */
    @Query("UPDATE ChatSession cs SET cs.isActive = false WHERE cs.updatedAt < :cutoffTime AND cs.isActive = true")
    int deactivateOldSessions(@Param("cutoffTime") LocalDateTime cutoffTime);

    /**
     * Find recent conversation sessions for a user (for conversation history)
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user AND cs.isActive = true AND cs.messageCount > 0 ORDER BY cs.updatedAt DESC")
    List<ChatSession> findRecentConversationsByUser(@Param("user") User user, Pageable pageable);

    /**
     * Find all active sessions for a user ordered by last activity
     */
    @Query("SELECT cs FROM ChatSession cs WHERE cs.user = :user AND cs.isActive = true ORDER BY cs.updatedAt DESC")
    List<ChatSession> findAllActiveSessionsByUser(@Param("user") User user);

    /**
     * Find session by token for any user (for session switching)
     */
    Optional<ChatSession> findBySessionToken(String sessionToken);
}
