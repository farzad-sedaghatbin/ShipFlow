package com.github.farzadsedaghatbin.shipflow.entity.github;

import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubPRState;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "github_pull_requests")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubPullRequest {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "repository_id", nullable = false)
  private GitHubRepository repository;

  @Column(name = "pr_number", nullable = false)
  private Integer prNumber;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 50)
  private GitHubPRState state;

  @Column(name = "head_branch", nullable = false)
  private String headBranch;

  @Column(name = "base_branch", nullable = false)
  private String baseBranch;

  @Column(name = "author_username")
  private String authorUsername;

  @Column(length = 1000)
  private String url;

  @Column(name = "opened_at", nullable = false)
  private LocalDateTime openedAt;

  @Column(name = "closed_at")
  private LocalDateTime closedAt;

  @Column(name = "merged_at")
  private LocalDateTime mergedAt;

  @Column(name = "merged_by_username")
  private String mergedByUsername;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
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
