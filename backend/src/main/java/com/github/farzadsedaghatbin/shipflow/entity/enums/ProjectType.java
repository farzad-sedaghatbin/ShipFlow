package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Defines the methodology/type for a project. This affects which features are
 * available and how navigation works.
 */
public enum ProjectType {
  /**
   * Shape Up methodology - 6-week cycles with betting, pitches, and cooldown.
   * Features: Cycles, Pitches, Betting Table, Hill Charts, Retrospectives
   */
  SHAPE_UP,

  /**
   * Kanban methodology - continuous flow with visual board. Features: Kanban
   * Board, Continuous Backlog, No cycles
   */
  KANBAN
}
