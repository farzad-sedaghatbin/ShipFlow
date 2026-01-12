package com.github.farzadsedaghatbin.shipflow.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubRepositoryDTO {
    private Long id;
    private String owner;
    private String name;
    private String fullName;
    private String url;
    private String defaultBranch;
    private Boolean isActive;
}
