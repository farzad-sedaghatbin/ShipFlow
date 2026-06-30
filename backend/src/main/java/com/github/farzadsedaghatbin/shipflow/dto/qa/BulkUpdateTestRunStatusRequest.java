package com.github.farzadsedaghatbin.shipflow.dto.qa;

import com.github.farzadsedaghatbin.shipflow.entity.enums.TestRunStatus;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

/** Request DTO for updating the status of many test runs at once (Zephyr-style bulk update). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkUpdateTestRunStatusRequest {

  @NotEmpty(message = "At least one test run ID is required")
  private List<Long> testRunIds;

  @NotNull(message = "Status is required")
  private TestRunStatus status;

  private String notes;
}
