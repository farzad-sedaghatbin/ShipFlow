package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.CommentReactionEntity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentReaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for CommentReactionEntity operations.
 */
@Repository
public interface CommentReactionRepository extends JpaRepository<CommentReactionEntity, Long> {

    /**
     * Find all reactions for a comment.
     */
    List<CommentReactionEntity> findByCommentId(Long commentId);

    /**
     * Find a specific reaction by user on a comment.
     */
    Optional<CommentReactionEntity> findByCommentIdAndUserIdAndReactionType(
            Long commentId, Long userId, CommentReaction reactionType);

    /**
     * Check if user has a specific reaction on a comment.
     */
    boolean existsByCommentIdAndUserIdAndReactionType(
            Long commentId, Long userId, CommentReaction reactionType);

    /**
     * Count reactions of a specific type on a comment.
     */
    @Query("SELECT COUNT(r) FROM CommentReactionEntity r WHERE r.comment.id = :commentId AND r.reactionType = :reactionType")
    long countByCommentIdAndReactionType(
            @Param("commentId") Long commentId,
            @Param("reactionType") CommentReaction reactionType);

    /**
     * Get all reaction counts for a comment grouped by type.
     */
    @Query("SELECT r.reactionType, COUNT(r) FROM CommentReactionEntity r WHERE r.comment.id = :commentId GROUP BY r.reactionType")
    List<Object[]> getReactionCountsByCommentId(@Param("commentId") Long commentId);

    /**
     * Get user's reactions on a comment.
     */
    @Query("SELECT r.reactionType FROM CommentReactionEntity r WHERE r.comment.id = :commentId AND r.user.id = :userId")
    List<CommentReaction> findUserReactionsByCommentIdAndUserId(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId);

    /**
     * Delete all reactions by a user on a comment.
     */
    void deleteByCommentIdAndUserId(Long commentId, Long userId);

    /**
     * Delete a specific reaction.
     */
    void deleteByCommentIdAndUserIdAndReactionType(Long commentId, Long userId, CommentReaction reactionType);
}
