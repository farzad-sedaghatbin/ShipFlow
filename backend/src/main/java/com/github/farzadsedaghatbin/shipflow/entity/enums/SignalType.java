package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Types of signals calculated across cycles for insight analysis.
 * These represent computed insights rather than raw metrics.
 */
public enum SignalType {
  /** Trend in appetite accuracy (estimated vs actual hours) */
  APPETITE_ACCURACY,
  
  /** Patterns in shaping quality (over/under-shaped pitches) */
  SHAPING_PATTERN,
  
  /** Correlation between early risk flags and final outcomes */
  RISK_CORRELATION,
  
  /** Rate of action items being followed through */
  RETRO_FOLLOW_THROUGH
}
