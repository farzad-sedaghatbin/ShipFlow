package com.github.farzadsedaghatbin.shipflow.entity.enums;

public enum TaskPriority {
  URGENT(4),
  HIGH(3),
  MEDIUM(2),
  LOW(1);

  private final int priority;

  TaskPriority(int priority) {
    this.priority = priority;
  }

  public int getPriority() {
    return priority;
  }
}
