package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Risk level indicators for releases.
 * Used to flag releases that may have delivery concerns.
 */
public enum ReleaseRiskLevel {
  /** On track, no significant concerns */
  LOW,

  /** Some concerns that need monitoring */
  MEDIUM,

  /** Significant risks that require attention */
  HIGH,

  /** Critical issues that threaten release delivery */
  CRITICAL
}
