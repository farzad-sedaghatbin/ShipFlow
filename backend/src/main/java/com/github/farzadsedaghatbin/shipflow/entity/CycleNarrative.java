package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.NarrativeType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Stores generated narrative summaries for cycles.
 * Narratives are either AI-generated or created from templates.
 * 
 * Supports the v0.5 "Insight, Not Metrics" philosophy by providing
 * human-readable summaries of what happened in a cycle.
 */
@Entity
@Table(name = "cycle_narratives")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleNarrative {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "cycle_id", nullable = false)
  private Cycle cycle;

  @Enumerated(EnumType.STRING)
  @Column(name = "narrative_type", nullable = false, length = 30)
  private NarrativeType narrativeType;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Column(name = "is_ai_generated")
  @Builder.Default
  private Boolean isAiGenerated = false;

  @Column(name = "ai_model", length = 100)
  private String aiModel;

  @Column(name = "generation_prompt_hash", length = 64)
  private String generationPromptHash;

  @Column(name = "generated_at", nullable = false)
  private LocalDateTime generatedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "generated_by_id")
  private User generatedBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    if (generatedAt == null) {
      generatedAt = createdAt;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
