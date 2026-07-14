package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Which context an AI-suggested task was grounded in.
 *
 * <p>Not persisted — used only on AI task-suggestion DTOs.
 */
public enum SuggestionSource {
  /** Grounded only in the pitch's text fields (problem, solution, appetite, etc). */
  PITCH,

  /** Also grounded in Figma design context fetched from the pitch's wireframe links. */
  PITCH_DESIGN
}
