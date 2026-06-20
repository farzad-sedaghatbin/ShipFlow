package com.github.farzadsedaghatbin.shipflow.entity.enums;

/** Types of entities that can be associated with knowledge items. */
public enum KnowledgeEntityType {
  PITCH,
  MEETING,
  WORKLOG,
  TEAM,
  CYCLE,
  EVIDENCE,
  MANUAL_NOTE,
  VALIDATED_QA,
  DOCUMENT,
  REFERENCE_DOCUMENT, // External reference materials like Shape Up methodology book
  INITIATIVE, // Strategic themes spanning multiple quarters
  EPIC, // Feature grouping layer between initiatives and pitches
  RELEASE, // Delivery milestones that can span multiple cycles
  KNOWLEDGE_SOURCE // Content imported via the Knowledge Center
}
