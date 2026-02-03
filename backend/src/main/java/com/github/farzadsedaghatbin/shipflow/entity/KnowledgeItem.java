package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity representing a piece of knowledge that can be embedded and searched.
 * Knowledge is extracted from various sources: pitches, meetings, worklogs,
 * notes, etc.
 */
@Entity
@Table(name = "knowledge_items", indexes = {@Index(name = "idx_knowledge_entity", columnList = "entityType, entityId"),
    @Index(name = "idx_knowledge_cycle", columnList = "cycleId"),
    @Index(name = "idx_knowledge_team", columnList = "teamId"),
    @Index(name = "idx_knowledge_embedded", columnList = "isEmbedded")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KnowledgeItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Type of entity this knowledge is derived from. */
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private KnowledgeEntityType entityType;

  /** ID of the source entity (pitch ID, meeting ID, etc.) */
  @Column(nullable = false)
  private Long entityId;

  /** The actual content/text of this knowledge chunk. */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  /** Title or summary of this knowledge chunk. */
  @Column(length = 500)
  private String title;

  /** ID of the associated cycle (for scoped retrieval). */
  private Long cycleId;

  /** ID of the associated team (for scoped retrieval). */
  private Long teamId;

  /** ID of the associated pitch (if applicable). */
  private Long pitchId;

  /** ID of the user who created/authored this content. */
  private Long authorId;

  /**
   * Unique identifier for the embedding in the vector store. Used to link this DB
   * record with the vector store entry.
   */
  @Column(length = 100)
  private String embeddingId;

  /** Whether this item has been embedded in the vector store. */
  @Builder.Default
  @Column(nullable = false)
  private Boolean isEmbedded = false;

  /** Chunk index for multi-chunk documents (0-based). */
  @Builder.Default
  @Column(nullable = false)
  private Integer chunkIndex = 0;

  /** Total number of chunks for this entity. */
  @Builder.Default
  @Column(nullable = false)
  private Integer totalChunks = 1;

  /** Hash of the content to detect changes. */
  @Column(length = 64)
  private String contentHash;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  /**
   * Build a unique embedding ID based on entity type, entity ID, and chunk index.
   */
  public String generateEmbeddingId() {
    return String.format("%s_%d_%d", entityType.name().toLowerCase(), entityId, chunkIndex);
  }
}
