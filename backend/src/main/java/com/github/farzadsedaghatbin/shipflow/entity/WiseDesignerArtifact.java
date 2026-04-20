package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Stores a single AI-generated HTML UI prototype artifact produced by the Wise Designer feature.
 *
 * <p>Each row represents one version of a clickable mockup for a pitch. The {@code htmlContent}
 * field is a self-contained HTML document (Tailwind CDN + inline JS) that can be rendered safely
 * inside a sandboxed {@code <iframe>}.
 *
 * <p>Multiple artifacts per pitch are supported — each call to generate or iterate creates a new
 * row so the full iteration history is preserved.
 */
@Entity
@Table(name = "wise_designer_artifacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WiseDesignerArtifact {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** The pitch this prototype was generated for. */
  @Column(name = "pitch_id", nullable = false)
  private Long pitchId;

  /** Self-contained HTML document — Tailwind CDN, inline JS, no external dependencies. */
  @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
  private String htmlContent;

  /** One-line description of what was generated / what changed in this iteration. */
  @Column(name = "description", length = 500)
  private String description;

  /**
   * Conversation session ID for follow-up iteration. Allows the LLM to receive the previous
   * HTML as context and apply targeted changes.
   */
  @Column(name = "session_id", length = 100)
  private String sessionId;

  /**
   * Optional: the Wise Architecture session ID that was used as design context. When set, the
   * architecture's component list and API contracts were injected into the design prompt.
   */
  @Column(name = "wise_arch_session_id", length = 100)
  private String wiseArchSessionId;

  @Column(name = "created_by_user_id")
  private Long createdByUserId;

  @CreationTimestamp
  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @UpdateTimestamp
  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;
}
