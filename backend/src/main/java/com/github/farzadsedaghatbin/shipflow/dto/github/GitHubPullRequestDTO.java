package com.github.farzadsedaghatbin.shipflow.dto.github;

import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubPRState;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubPullRequestDTO {
  private Long id;
  private Integer prNumber;
  private String title;
  private String description;
  private GitHubPRState state;
  private String headBranch;
  private String baseBranch;
  private String authorUsername;
  private String url;
  private LocalDateTime openedAt;
  private LocalDateTime closedAt;
  private LocalDateTime mergedAt;
  private String mergedByUsername;
  private String repositoryFullName;
}
