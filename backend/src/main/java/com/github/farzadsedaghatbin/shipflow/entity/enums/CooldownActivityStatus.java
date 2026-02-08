package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Status of a cooldown activity.
 */
public enum CooldownActivityStatus {
  /** Activity is planned but not yet started */
  PLANNED,

  /** Activity is currently in progress */
  IN_PROGRESS,

  /** Activity has been completed */
  COMPLETED,

  /** Activity was skipped or deprioritized */
  SKIPPED,

  /** Activity was blocked by something */
  BLOCKED
}
