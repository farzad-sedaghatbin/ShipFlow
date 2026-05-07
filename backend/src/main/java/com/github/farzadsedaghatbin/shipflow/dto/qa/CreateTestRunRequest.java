package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.*;

/** Request DTO for creating a test run. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTestRunRequest {

  @NotNull(message = "Test case ID is required")
  private Long testCaseId;

  private Long cycleId;

  private Long pitchId;

  @NotNull(message = "Status is required")
  private TestRunStatus status;

  private LocalDateTime executedAt;

  private Integer durationSeconds;

  private String notes;

  private String actualResult;

  private String buildVersion;

  private String environment;

  private String attachments;
}
