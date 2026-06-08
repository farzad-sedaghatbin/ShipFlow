package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Types of narrative summaries generated for a cycle.
 * Supports the "Insight, Not Metrics" philosophy in v0.5.
 */
public enum NarrativeType {
  /** What pitches were committed to in the betting table */
  WHAT_WE_BET,
  
  /** What pitches were completed and shipped */
  WHAT_SHIPPED,
  
  /** What pitches were cut (circuit breaker or cancelled) */
  WHAT_WE_CUT,
  
  /** Unexpected outcomes: over/under budget, surprise risks */
  SURPRISES,
  
  /** Full cycle summary combining all narrative types */
  FULL_SUMMARY,

  /** AI-generated summary of a retrospective board (went well, blockers, action items) */
  RETROSPECTIVE_SUMMARY
}
