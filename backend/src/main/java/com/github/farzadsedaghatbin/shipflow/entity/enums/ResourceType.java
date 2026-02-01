package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Enumeration of resources that can have permission controls. Each resource type represents a
 * domain entity in the system.
 */
public enum ResourceType {
  /** Cycle management resource */
  CYCLE,

  /** Pitch/proposal management resource */
  PITCH,

  /** Bug tracking resource */
  BUG,

  /** Report generation and viewing resource */
  REPORT,

  /** Project management resource */
  PROJECT,

  /** Team management resource */
  TEAM,

  /** User administration resource */
  USER,

  /** Risk feedback resource */
  RISK,

  /** Dashboard and widgets resource */
  DASHBOARD,

  /** Retrospective resource */
  RETROSPECTIVE,

  /** Betting table resource */
  BETTING_TABLE,

  /** AI/Q&A features resource */
  AI_FEATURES,

  /** Global system settings */
  SYSTEM
}
