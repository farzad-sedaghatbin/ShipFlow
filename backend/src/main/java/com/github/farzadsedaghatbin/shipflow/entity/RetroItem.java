package com.github.farzadsedaghatbin.shipflow.entity;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RetroColumnType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "retro_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetroItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /**
   * Optimistic-locking version (JPA {@code @Version}). Boxed {@code Long} so a brand-new,
   * unsaved entity's version is distinguishably {@code null} rather than {@code 0} colliding
   * with a real first-save value.
   */
  @Version
  @Column(nullable = false)
  private Long version;

  @Column(nullable = false, columnDefinition = "TEXT")
  private String content;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private RetroColumnType columnType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "retrospective_id", nullable = false)
  private Retrospective retrospective;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "author_id")
  private User author;

  @Builder.Default
  @Column(nullable = false)
  private Boolean isAnonymous = false;

  @Builder.Default
  @Column(nullable = false)
  private Integer voteCount = 0;

  @Builder.Default
  @Column(name = "dislike_count", nullable = false)
  private Integer dislikeCount = 0;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "merged_into_id")
  private RetroItem mergedInto;

  @Builder.Default
  @OneToMany(mappedBy = "mergedInto", cascade = CascadeType.ALL)
  private List<RetroItem> mergedItems = new ArrayList<>();

  @Builder.Default
  @OneToMany(mappedBy = "retroItem", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<RetroItemVote> votes = new ArrayList<>();

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column
  private LocalDateTime updatedAt;

  // Discussion tracking
  @Builder.Default
  @Column(name = "discussed", nullable = false)
  private Boolean discussed = false;

  @Column(name = "discussed_at")
  private LocalDateTime discussedAt;

  // v0.5 - Action follow-through tracking
  @Builder.Default
  @Column(name = "acted_on")
  private Boolean actedOn = false;

  @Column(name = "acted_on_notes", columnDefinition = "TEXT")
  private String actedOnNotes;

  @Column(name = "acted_on_at")
  private LocalDateTime actedOnAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "acted_on_by_id")
  private User actedOnBy;

  // v0.5 - Tag-based correlation
  @Builder.Default
  @ManyToMany
  @JoinTable(
      name = "retro_item_tags",
      joinColumns = @JoinColumn(name = "retro_item_id"),
      inverseJoinColumns = @JoinColumn(name = "tag_id"))
  private Set<Tag> tags = new HashSet<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
