package com.github.farzadsedaghatbin.shipflow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Velocity data for a single sprint/cycle. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VelocityPointDTO {
  private Long cycleId;
  private String cycleName;
  /** Sum of story points for all tasks in the cycle (planned capacity). */
  private Integer plannedPoints;
  /** Sum of story points for tasks in DONE status within the cycle. */
  private Integer completedPoints;
}
