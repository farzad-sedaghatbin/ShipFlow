package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Polymorphic link between a domain entity (Pitch or Task) and a {@link WikiPage} kept for
 * reference (research/docs). Mirrors the {@code entityType}/{@code entityId} convention used by
 * {@link UploadedDocument} and {@code Comment} rather than per-entity join tables, while keeping a
 * real FK to the wiki page (see {@code entity/github/PitchGitHubLink.java} /
 * {@code TaskGitHubLink.java} for the precedent of a real FK + {@code linkedAt} + {@code
 * linkedByUser}).
 *
 * <p>{@link WikiPage} uses soft delete ({@code deletedAt}), so there is no DB-level cascade here —
 * read queries must exclude links pointing at a soft-deleted wiki page.
 */
@Entity
@Table(name = "entity_wiki_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EntityWikiLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** "PITCH" or "TASK". Bug reports are explicitly out of scope. */
  @Column(name = "entity_type", nullable = false, length = 20)
  private String entityType;

  @Column(name = "entity_id", nullable = false)
  private Long entityId;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "wiki_page_id", nullable = false)
  private WikiPage wikiPage;

  @Column(name = "linked_at", nullable = false)
  private LocalDateTime linkedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_by_user_id")
  private User linkedBy;

  @PrePersist
  protected void onCreate() {
    if (linkedAt == null) {
      linkedAt = LocalDateTime.now();
    }
  }
}
