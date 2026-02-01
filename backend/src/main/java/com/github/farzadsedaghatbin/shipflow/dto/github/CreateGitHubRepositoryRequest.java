package com.github.farzadsedaghatbin.shipflow.dto.github;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGitHubRepositoryRequest {
  @NotBlank(message = "Owner is required")
  private String owner;

  @NotBlank(message = "Repository name is required")
  private String name;

  private String url;
  private String defaultBranch;
  private String webhookSecret;
  private String accessToken;
  private Boolean autoLinkEnabled;
  private Boolean autoCloseTasksOnMerge;
}
