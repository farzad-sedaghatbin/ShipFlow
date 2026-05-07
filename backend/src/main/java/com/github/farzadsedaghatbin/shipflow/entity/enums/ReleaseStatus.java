package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Status values for releases/milestones.
 * Releases represent delivery milestones that can contain items from multiple epics.
 */
public enum ReleaseStatus {
  /** Release is being planned, scope not finalized */
  PLANNING,

  /** Development work is in progress */
  IN_PROGRESS,

  /** Release is in staging/testing environment */
  STAGING,

  /** Release has been deployed to production */
  RELEASED,

  /** Release was cancelled and will not be deployed */
  CANCELLED
}
