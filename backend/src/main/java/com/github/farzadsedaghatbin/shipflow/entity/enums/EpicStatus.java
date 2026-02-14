package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Status values for epics in the roadmap.
 * Epics are large features that group related pitches together.
 */
public enum EpicStatus {
  /** Initial draft state, not yet approved for planning */
  DRAFT,

  /** Approved and scheduled for future work */
  PLANNED,

  /** Currently being worked on with active pitches */
  IN_PROGRESS,

  /** All work completed successfully */
  COMPLETED,

  /** Temporarily paused, may resume later */
  ON_HOLD,

  /** Permanently cancelled, will not be completed */
  CANCELLED
}
