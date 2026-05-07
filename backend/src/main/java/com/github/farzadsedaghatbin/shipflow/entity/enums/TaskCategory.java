package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Category of a task to differentiate between pitch-scoped work and technical
 * debt/improvements.
 *
 * <p>
 * PITCH_SCOPE - Tasks that are scoped to and tracked against a specific pitch
 * DEBT_IMPROVEMENT - Technical debt, maintenance, or improvement tasks not tied
 * to specific pitches
 */
public enum TaskCategory {
  /** Tasks scoped to a pitch - tracked against pitch progress */
  PITCH_SCOPE,

  /** Technical debt, maintenance, or improvement tasks */
  DEBT_IMPROVEMENT
}
