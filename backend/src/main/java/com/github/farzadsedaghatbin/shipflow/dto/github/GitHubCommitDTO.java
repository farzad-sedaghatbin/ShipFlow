package com.github.farzadsedaghatbin.shipflow.dto.github;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubCommitDTO {
    private Long id;
    private String sha;
    private String message;
    private String authorName;
    private String authorEmail;
    private String authorUsername;
    private LocalDateTime commitDate;
    private String branch;
    private String url;
    private String repositoryFullName;
}
