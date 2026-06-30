package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Lifecycle status of a knowledge source ingestion. */
public enum KnowledgeSourceStatus {
  PENDING,
  INGESTING,
  READY,
  FAILED,
  STALE
}
