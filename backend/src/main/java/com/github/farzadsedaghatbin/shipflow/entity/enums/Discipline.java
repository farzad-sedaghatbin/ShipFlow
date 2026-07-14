package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Delivery stack involved in building an AI-suggested deliverable task.
 *
 * <p>Not persisted on {@code Task} — used only on AI task-suggestion DTOs so the UI can show which
 * disciplines collaborate on a suggested deliverable before it's created.
 */
public enum Discipline {
  DESIGN,
  BACKEND,
  MOBILE,
  QA
}
