package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.WiseArchitectureAdvice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Repository for WISE Architecture advice history.
 */
@Repository
public interface WiseArchitectureAdviceRepository extends JpaRepository<WiseArchitectureAdvice, Long> {

    /**
     * Find all advice entries for a user, ordered by most recent.
     */
    Page<WiseArchitectureAdvice> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Find all advice entries for a pitch, ordered by most recent.
     */
    List<WiseArchitectureAdvice> findByPitchIdOrderByCreatedAtDesc(Long pitchId);

    /**
     * Find all messages in a conversation, ordered chronologically.
     */
    List<WiseArchitectureAdvice> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /**
     * Find distinct conversations for a user (returns the latest message from each).
     * Uses @EntityGraph to eagerly fetch pitch and its cycle/project to avoid N+1 queries.
     */
    @EntityGraph(attributePaths = {"pitch", "pitch.cycle", "pitch.cycle.project"})
    @Query("""
        SELECT w FROM WiseArchitectureAdvice w 
        WHERE w.userId = :userId 
        AND w.messageType = 'INITIAL_SOLUTION'
        ORDER BY w.createdAt DESC
        """)
    Page<WiseArchitectureAdvice> findConversationsByUserId(@Param("userId") Long userId, Pageable pageable);

    /**
     * Find distinct conversations for a pitch (returns the latest message from each).
     * Uses @EntityGraph to eagerly fetch pitch and its cycle/project to avoid N+1 queries.
     */
    @EntityGraph(attributePaths = {"pitch", "pitch.cycle", "pitch.cycle.project"})
    @Query("""
        SELECT w FROM WiseArchitectureAdvice w 
        WHERE w.pitch.id = :pitchId 
        AND w.messageType = 'INITIAL_SOLUTION'
        ORDER BY w.createdAt DESC
        """)
    List<WiseArchitectureAdvice> findConversationsByPitchId(@Param("pitchId") Long pitchId);

    /**
     * Count total conversations for a user.
     */
    @Query("SELECT COUNT(DISTINCT w.conversationId) FROM WiseArchitectureAdvice w WHERE w.userId = :userId")
    long countConversationsByUserId(@Param("userId") Long userId);

    /**
     * Count messages in a conversation.
     */
    long countByConversationId(String conversationId);

    /**
     * Count messages for multiple conversations in a single query.
     * Returns a list of Object[] where [0] is conversationId and [1] is count.
     */
    @Query("""
        SELECT w.conversationId, COUNT(w) 
        FROM WiseArchitectureAdvice w 
        WHERE w.conversationId IN :conversationIds 
        GROUP BY w.conversationId
        """)
    List<Object[]> countMessagesByConversationIds(@Param("conversationIds") Set<String> conversationIds);
}
