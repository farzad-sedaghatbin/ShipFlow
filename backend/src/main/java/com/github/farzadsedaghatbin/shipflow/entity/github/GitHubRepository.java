package com.github.farzadsedaghatbin.shipflow.entity.github;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "github_repositories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GitHubRepository {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String owner;

  @Column(nullable = false)
  private String name;

  @Column(name = "full_name", nullable = false, length = 511)
  private String fullName;

  @Column(length = 1000)
  private String url;

  @Column(name = "default_branch")
  private String defaultBranch;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (isActive == null) {
      isActive = true;
    }
    if (defaultBranch == null) {
      defaultBranch = "main";
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
