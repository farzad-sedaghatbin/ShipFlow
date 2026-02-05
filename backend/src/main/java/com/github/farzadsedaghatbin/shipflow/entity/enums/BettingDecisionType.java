package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Type of betting decision made during a betting meeting.
 * Tracks whether a pitch was committed to or rejected, with reasoning.
 */
public enum BettingDecisionType {
  /** Pitch is committed to for the cycle - team will work on it */
  COMMITTED,
  
  /** Pitch is rejected for this cycle - won't be worked on */
  REJECTED,
  
  /** Pitch is deferred to a future cycle - needs more shaping or timing isn't right */
  DEFERRED,
  
  /** Pitch needs more shaping before it can be bet on */
  NEEDS_SHAPING
}
