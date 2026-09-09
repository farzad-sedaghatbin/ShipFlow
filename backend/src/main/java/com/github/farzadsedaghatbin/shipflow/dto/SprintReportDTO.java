package com.github.farzadsedaghatbin.shipflow.dto;

import java.time.LocalDate;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Per-sprint summary report combining velocity, completion rate, and scope-change data. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SprintReportDTO {
  private Long cycleId;
  private String cycleName;
  private LocalDate startDate;
  private LocalDate endDate;

  /** Sum of story points for all tasks in the cycle (planned capacity). */
  private Integer plannedPoints;

  /** Sum of story points for tasks in DONE status within the cycle. */
  private Integer completedPoints;

  /** completedPoints / plannedPoints, or 0.0 when plannedPoints is 0. */
  private Double completionRate;

  /** Count of tasks in the cycle per {@code TaskStatus}, keyed by enum name. */
  private Map<String, Integer> taskCountByStatus;

  /** Number of tasks whose createdAt is after the cycle's startDate. */
  private Integer scopeAddedTaskCount;

  /** Sum of story points for tasks whose createdAt is after the cycle's startDate. */
  private Integer scopeAddedPoints;
}
