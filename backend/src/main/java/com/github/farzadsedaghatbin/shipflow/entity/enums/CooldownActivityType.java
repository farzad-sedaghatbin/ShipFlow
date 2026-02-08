package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Types of activities that can be tracked during the cooldown phase of a Shape Up cycle.
 * Cooldown is the 2-week period between build cycles for cleanup and preparation.
 */
public enum CooldownActivityType {
  /** Technical debt repayment - refactoring, code cleanup, performance improvements */
  TECH_DEBT,

  /** Bug fixes - addressing bugs found during or after the build phase */
  BUG_FIX,

  /** Code refactoring - improving code structure without changing functionality */
  REFACTORING,

  /** Kickoff preparation - preparing for the next cycle's shaping/betting */
  KICKOFF_PREP,

  /** Learning - team training, documentation reading, skill development */
  LEARNING,

  /** Quality assurance - additional testing, test coverage improvements */
  QA,

  /** Documentation - writing or updating technical or user documentation */
  DOCUMENTATION,

  /** Infrastructure - DevOps, deployment improvements, environment setup */
  INFRASTRUCTURE,

  /** Research - investigating new technologies or solutions for future work */
  RESEARCH,

  /** Planning - roadmap discussions, strategic planning for future cycles */
  PLANNING,

  /** Other - catch-all for activities that don't fit other categories */
  OTHER
}
