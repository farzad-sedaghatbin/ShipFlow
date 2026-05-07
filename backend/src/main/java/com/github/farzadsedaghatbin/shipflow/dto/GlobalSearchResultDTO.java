package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO representing a single result from the global search endpoint.
 * Each result corresponds to a searchable entity (Task, Subtask, BugReport, Pitch, Epic).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GlobalSearchResultDTO {

  /** The type of entity: TASK, SUBTASK, BUG_REPORT, PITCH, EPIC */
  private String entityType;

  /** The database ID of the entity */
  private Long entityId;

  /** Display title (task title, epic name, bug title, pitch title) */
  private String title;

  /** Contextual subtitle (e.g. status, bug key, cycle name) */
  private String subtitle;

  /** Frontend route path for direct navigation (e.g. /backlog/42, /epics/7) */
  private String route;

  /** Relevance score for ranking (higher = more relevant) */
  private Double score;

  /** How the match was found: EXACT_KEY, TRIGRAM */
  private String matchedBy;
}
