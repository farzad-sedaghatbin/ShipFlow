package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Entity for storing manual notes added by users. These can be associated with pitches, meetings,
 * teams, or cycles.
 */
@Entity
@Table(
    name = "manual_notes",
    indexes = {
      @Index(name = "idx_note_context", columnList = "contextType, contextId"),
      @Index(name = "idx_note_author", columnList = "authorId")
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualNote {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Title of the note. */
  @Column(nullable = false, length = 255)
  private String title;

  /** Content of the note. */
  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;

  /** Context type (pitch, meeting, team, cycle). */
  @Column(nullable = false, length = 50)
  private String contextType;

  /** ID of the context entity. */
  @Column(nullable = false)
  private Long contextId;

  /** ID of the associated cycle. */
  private Long cycleId;

  /** ID of the associated team. */
  private Long teamId;

  /** ID of the associated pitch. */
  private Long pitchId;

  /** ID of the user who created the note. */
  @Column(nullable = false)
  private Long authorId;

  /** Whether this note should be included in knowledge base. */
  @Builder.Default
  @Column(nullable = false)
  private Boolean includeInKnowledge = true;

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
}
