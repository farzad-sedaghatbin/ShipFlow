package com.github.farzadsedaghatbin.shipflow.entity.github;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "github_configuration")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubConfiguration {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "repository_id", nullable = false, unique = true)
  private GitHubRepository repository;

  @Column(name = "webhook_secret", length = 500)
  private String webhookSecret;

  @Column(name = "access_token", length = 500)
  private String accessToken;

  @Column(name = "auto_link_enabled", nullable = false)
  private Boolean autoLinkEnabled;

  @Column(name = "auto_close_tasks_on_merge", nullable = false)
  private Boolean autoCloseTasksOnMerge;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (autoLinkEnabled == null) {
      autoLinkEnabled = true;
    }
    if (autoCloseTasksOnMerge == null) {
      autoCloseTasksOnMerge = true;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
