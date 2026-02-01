package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Risk level categories for pitches and cycles. Thresholds are configurable via
 * OrganizationSettings.
 */
public enum RiskLevel {
  LOW, // Score <= lowMax (default: 30)
  MEDIUM, // Score > lowMax && <= mediumMax (default: 31-60)
  HIGH, // Score > mediumMax && <= highMax (default: 61-85)
  CRITICAL // Score > highMax (default: > 85)
}
