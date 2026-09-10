package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BugSeverity;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BugStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.*;

/** Request DTO for creating a bug report. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateBugReportRequest {

  @NotBlank(message = "Title is required")
  @Size(max = 255, message = "Title must be less than 255 characters")
  private String title;

  @NotBlank(message = "Description is required")
  private String description;

  private String stepsToReproduce;

  private String expectedBehavior;

  private String actualBehavior;

  private String environment;

  private String component;

  /** Direct project association - required for Kanban, optional for Shape Up. */
  private Long projectId;

  private Long pitchId;

  private Long cycleId;

  private Long teamId;

  private Long testRunId;

  private Long taskId;

  @NotNull(message = "Severity is required")
  private BugSeverity severity;

  private BugStatus status;

  private List<String> tags;

  private String attachments;

  private Long assigneeId;

  /** Person assigned to QA/test this bug. */
  private Long qaAssigneeId;

  private Long targetReleaseId;

  /**
   * Optional client-generated key (e.g. a UUID minted once when the form is submitted) that makes
   * this request idempotent — a retried request carrying the same key returns the
   * already-created bug report instead of creating a duplicate. See {@code BugReport.idempotencyKey}.
   */
  @Size(max = 100, message = "Idempotency key must be less than 100 characters")
  private String idempotencyKey;
}
