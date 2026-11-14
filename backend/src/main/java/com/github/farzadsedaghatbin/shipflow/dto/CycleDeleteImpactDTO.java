package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.*;

/**
 * What deleting a cycle would permanently remove. Returned by
 * {@code GET /api/cycles/{id}/delete-impact} so the UI can warn before the cascade runs.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleDeleteImpactDTO {
  private Long cycleId;
  private String cycleName;
  /** Tasks in the cycle (not soft-deleted) — they are hard-deleted along with it. */
  private int taskCount;
  /** Tasks in the cycle that are already DONE. */
  private int doneTaskCount;
  /** Pitches (not soft-deleted) belonging to the cycle. */
  private int pitchCount;
  /** Retrospectives of the cycle. */
  private int retrospectiveCount;
}
