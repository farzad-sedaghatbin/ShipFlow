package com.github.farzadsedaghatbin.shipflow.entity.github;

import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "task_github_links")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskGitHubLink {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "task_id", nullable = false)
  private Task task;

  @Enumerated(EnumType.STRING)
  @Column(name = "link_type", nullable = false, length = 50)
  private GitHubLinkType linkType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "commit_id")
  private GitHubCommit commit;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "pull_request_id")
  private GitHubPullRequest pullRequest;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id")
  private GitHubBranch branch;

  @Column(name = "linked_at", nullable = false)
  private LocalDateTime linkedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "linked_by_user_id")
  private User linkedByUser;

  @Column(name = "auto_linked", nullable = false)
  private Boolean autoLinked;

  @PrePersist
  protected void onCreate() {
    linkedAt = LocalDateTime.now();
    if (autoLinked == null) {
      autoLinked = false;
    }
  }
}
