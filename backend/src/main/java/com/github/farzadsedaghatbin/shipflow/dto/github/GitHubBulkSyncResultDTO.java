package com.github.farzadsedaghatbin.shipflow.dto.github;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO for bulk repository sync status */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GitHubBulkSyncResultDTO {

  /** Total number of installations processed */
  private int installationsProcessed;

  /** Number of repositories discovered */
  private int repositoriesDiscovered;

  /** Number of new repositories added */
  private int repositoriesAdded;

  /** Number of existing repositories updated */
  private int repositoriesUpdated;

  /** Number of repositories that failed to sync */
  private int repositoriesFailed;

  /** List of errors encountered during sync */
  private List<String> errors;

  /** Overall success status */
  private boolean success;

  /** Summary message */
  private String message;
}
