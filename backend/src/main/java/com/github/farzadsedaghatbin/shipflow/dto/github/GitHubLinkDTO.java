package com.github.farzadsedaghatbin.shipflow.dto.github;

import com.github.farzadsedaghatbin.shipflow.entity.enums.GitHubLinkType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubLinkDTO {
  private Long id;
  private GitHubLinkType linkType;
  private GitHubCommitDTO commit;
  private GitHubPullRequestDTO pullRequest;
  private GitHubBranchDTO branch;
  private LocalDateTime linkedAt;
  private String linkedByUsername;
  private Boolean autoLinked; // Only for task links
}
