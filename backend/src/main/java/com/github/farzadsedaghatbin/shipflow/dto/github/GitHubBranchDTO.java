package com.github.farzadsedaghatbin.shipflow.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubBranchDTO {
    private Long id;
    private String name;
    private String headSha;
    private Boolean isDefault;
    private String repositoryFullName;
}
