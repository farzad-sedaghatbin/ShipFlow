package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/** DTO for bug report responses. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BugReportDTO {
  private Long id;
  private String bugKey;
  private String title;
  private String description;
  private String stepsToReproduce;
  private String expectedBehavior;
  private String actualBehavior;
  private String environment;
  private String component;

  // Direct project association
  private Long projectId;
  private String projectName;
  private String projectKey;

  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;
  private Long teamId;
  private String teamName;
  private Long testRunId;

  // Task relationship - tasks are now integrated with scopes
  private Long taskId;
  private String taskTitle;

  private BugSeverity severity;
  private BugStatus status;
  private String tags;
  private List<String> tagList;
  private String attachments;
  private Long reporterId;
  private String reporterName;
  private Long assigneeId;
  private String assigneeName;
  private String resolution;
  private LocalDateTime resolvedAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  // Release tracking
  private Long targetReleaseId;
  private String targetReleaseName;
  private String targetReleaseVersion;
  private Long fixedInReleaseId;
  private String fixedInReleaseName;
  private String fixedInReleaseVersion;
  /** True if bug was fixed in a later release than target */
  private Boolean isSlipped;

  // Comment count
  private Integer commentCount;
}
