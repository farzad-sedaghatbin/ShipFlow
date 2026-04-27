package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Status progression for pitches in Shape Up methodology.
 * 
 * Pre-cycle statuses (no cycle required):
 * - IDEA: Raw concept, just a title - lightweight capture
 * - DRAFT: Being shaped - problem/solution being defined
 * - SHAPED: Complete pitch ready for betting table
 * 
 * In-cycle statuses (cycle required):
 * - PENDING: Assigned to cycle, waiting to start
 * - STARTED: Kicked off work
 * - IN_PROGRESS: Active development
 * - TESTING: QA phase
 * - DONE: Completed successfully
 * - COOLDOWN: Post-cycle refinement
 * - CANCELLED: Abandoned
 * - CIRCUIT_BREAKER: Exceeded time budget - Shape Up safety valve
 */
public enum PitchStatus {
  // Pre-cycle statuses (no cycle required)
  IDEA,           // Raw concept - just title, optional description
  DRAFT,          // Being shaped - work in progress on problem/solution
  SHAPED,         // Ready for betting - complete pitch with appetite
  
  // In-cycle statuses (cycle required)
  PENDING,        // Assigned to cycle, ready to start
  STARTED,        // Kicked off
  IN_PROGRESS,    // Active development  
  TESTING,        // QA phase
  DONE,           // Completed
  COOLDOWN,       // Post-cycle refinement
  CANCELLED,      // Abandoned
  CIRCUIT_BREAKER // Exceeded time budget - Shape Up safety valve
}
