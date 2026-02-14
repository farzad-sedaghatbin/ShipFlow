package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Status values for strategic initiatives in the roadmap.
 * Initiatives represent high-level strategic themes that span multiple quarters.
 */
public enum InitiativeStatus {
  /** Initial draft state, not yet approved for planning */
  DRAFT,

  /** Approved and scheduled for future work */
  PLANNED,

  /** Currently being worked on with active epics */
  IN_PROGRESS,

  /** All work completed successfully */
  COMPLETED,

  /** Temporarily paused, may resume later */
  ON_HOLD,

  /** Permanently cancelled, will not be completed */
  CANCELLED
}
