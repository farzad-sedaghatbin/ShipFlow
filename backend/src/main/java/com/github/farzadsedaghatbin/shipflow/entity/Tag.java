package com.github.farzadsedaghatbin.shipflow.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

/**
 * Tag entity for cross-entity correlation between retro items and pitches.
 * Enables linking retrospective insights to future shaping/betting decisions.
 */
@Entity
@Table(name = "tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tag {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "project_id", nullable = false)
  private Project project;

  @Column(length = 20)
  @Builder.Default
  private String color = "#6366f1";

  @Column(columnDefinition = "TEXT")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by_id")
  private User createdBy;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Builder.Default
  @ManyToMany(mappedBy = "tags")
  private Set<RetroItem> retroItems = new HashSet<>();

  @Builder.Default
  @ManyToMany(mappedBy = "tags")
  private Set<Pitch> pitches = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
