package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

/**
 * Request DTO for recording the same execution result against many test cases at once (Zephyr-style
 * bulk execution). One {@code TestRun} is created per test case with the shared status/environment.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRecordTestRunsRequest {

  @NotEmpty(message = "At least one test case ID is required")
  private List<Long> testCaseIds;

  @NotNull(message = "Status is required")
  private TestRunStatus status;

  /** Environment the tests were executed against (e.g. "Chrome / staging"). Applied to every run. */
  private String environment;

  private String buildVersion;

  private String notes;

  private Long cycleId;

  private Long pitchId;
}
