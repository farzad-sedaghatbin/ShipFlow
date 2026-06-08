package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Lifecycle states of a CSV import job. */
public enum ImportJobStatus {
  PENDING,
  PARSING,
  IMPORTING,
  COMPLETED,
  FAILED
}
