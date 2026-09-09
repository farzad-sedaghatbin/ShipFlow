package com.github.farzadsedaghatbin.shipflow.dto;

import com.github.farzadsedaghatbin.shipflow.entity.enums.ReleaseStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Release-level report data for a single release/milestone in a project. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReleaseReportDTO {
  private Long releaseId;
  private String releaseName;
  private String version;
  private ReleaseStatus status;
  private LocalDate targetDate;
  private LocalDate releaseDate;

  private Integer taskCount;
  private Integer completedTaskCount;

  /** Sum of story points for all tasks targeting the release (planned scope). */
  private Integer plannedPoints;

  /** Sum of story points for tasks in DONE status targeting the release. */
  private Integer completedPoints;
}
