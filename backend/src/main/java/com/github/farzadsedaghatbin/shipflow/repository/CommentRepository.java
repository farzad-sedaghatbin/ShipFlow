package com.github.farzadsedaghatbin.shipflow.repository;

import com.github.farzadsedaghatbin.shipflow.entity.Comment;
import com.github.farzadsedaghatbin.shipflow.entity.enums.CommentEntityType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Comment entity operations.
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

  /**
   * Find all non-deleted comments for a specific entity.
   */
  @Query("SELECT c FROM Comment c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.deletedAt IS NULL ORDER BY c.createdAt ASC")
  List<Comment> findByEntityTypeAndEntityIdNotDeleted(@Param("entityType") CommentEntityType entityType,
      @Param("entityId") Long entityId);

  /**
   * Find all non-deleted comments for a specific entity with pagination.
   */
  @Query("SELECT c FROM Comment c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.deletedAt IS NULL")
  Page<Comment> findByEntityTypeAndEntityIdNotDeleted(@Param("entityType") CommentEntityType entityType,
      @Param("entityId") Long entityId, Pageable pageable);

  /**
   * Find a non-deleted comment by ID.
   */
  @Query("SELECT c FROM Comment c WHERE c.id = :id AND c.deletedAt IS NULL")
  Optional<Comment> findByIdNotDeleted(@Param("id") Long id);

  /**
   * Count non-deleted comments for an entity.
   */
  @Query("SELECT COUNT(c) FROM Comment c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.deletedAt IS NULL")
  long countByEntityTypeAndEntityIdNotDeleted(@Param("entityType") CommentEntityType entityType,
      @Param("entityId") Long entityId);

  /**
   * Find comments by author.
   */
  @Query("SELECT c FROM Comment c WHERE c.author.id = :authorId AND c.deletedAt IS NULL ORDER BY c.createdAt DESC")
  List<Comment> findByAuthorIdNotDeleted(@Param("authorId") Long authorId);

  /**
   * Check if entity exists (task or bug report has comments).
   */
  @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM Comment c WHERE c.entityType = :entityType AND c.entityId = :entityId AND c.deletedAt IS NULL")
  boolean existsByEntityTypeAndEntityIdNotDeleted(@Param("entityType") CommentEntityType entityType,
      @Param("entityId") Long entityId);
}
