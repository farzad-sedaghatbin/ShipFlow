package com.github.farzadsedaghatbin.shipflow.service.vcs;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Generic interface for Version Control System (VCS) providers.
 *
 * <p>Implementations of this interface encapsulate provider-specific logic for
 * processing commits, pull/merge requests and managing repository
 * configurations. The application can support multiple VCS providers (GitHub,
 * GitLab, Bitbucket, etc.) through different implementations.</p>
 *
 * @since 0.6.0
 */
public interface VCSProvider {

  /**
   * Returns the unique name of this VCS provider (e.g. "github", "gitlab").
   */
  String getProviderName();

  /**
   * Process and persist a commit received from a webhook or sync event.
   *
   * <p>Implementations should handle deduplication (idempotent by SHA) and,
   * when auto-linking is enabled, automatically link the commit to tasks and
   * pitches referenced in the commit message.</p>
   *
   * @param repositoryFullName full name of the repository (e.g. "owner/repo")
   * @param sha                commit SHA
   * @param message            commit message
   * @param authorName         display name of the author
   * @param authorEmail        email of the author
   * @param authorUsername     VCS username of the author
   * @param commitDate         timestamp of the commit
   * @param branch             branch the commit was pushed to
   * @param url                web URL of the commit
   */
  void processCommit(String repositoryFullName, String sha, String message,
      String authorName, String authorEmail, String authorUsername,
      LocalDateTime commitDate, String branch, String url);

  /**
   * Process and persist a pull/merge request received from a webhook or sync
   * event.
   *
   * <p>Implementations should handle deduplication (idempotent by repository +
   * PR number), auto-link to tasks/pitches, and optionally auto-close tasks
   * when the PR is merged using conventional close keywords.</p>
   *
   * @param repositoryFullName full name of the repository
   * @param prNumber           pull/merge request number
   * @param title              title of the PR/MR
   * @param description        body/description (nullable)
   * @param state              current state (OPEN, CLOSED, MERGED)
   * @param headBranch         source branch
   * @param baseBranch         target branch
   * @param authorUsername     VCS username of the author
   * @param url                web URL of the PR/MR
   * @param openedAt           when the PR/MR was opened
   * @param closedAt           when the PR/MR was closed (nullable)
   * @param mergedAt           when the PR/MR was merged (nullable)
   * @param mergedByUsername   who merged it (nullable)
   */
  void processPullRequest(String repositoryFullName, Integer prNumber,
      String title, String description, String state,
      String headBranch, String baseBranch, String authorUsername, String url,
      LocalDateTime openedAt, LocalDateTime closedAt, LocalDateTime mergedAt,
      String mergedByUsername);

  /**
   * Retrieve all VCS link DTOs for a given task.
   *
   * @param taskId task primary key
   * @return list of link DTOs (commits, PRs, branches)
   */
  List<?> getTaskLinks(Long taskId);

  /**
   * Retrieve all VCS link DTOs for a given pitch.
   *
   * @param pitchId pitch primary key
   * @return list of link DTOs (commits, PRs, branches)
   */
  List<?> getPitchLinks(Long pitchId);
}
