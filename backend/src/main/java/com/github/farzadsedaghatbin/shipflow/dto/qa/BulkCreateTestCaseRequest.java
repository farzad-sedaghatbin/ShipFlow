package com.github.farzadsedaghatbin.shipflow.dto.qa;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Request DTO for bulk-creating test cases in a single atomic operation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkCreateTestCaseRequest {

  @NotEmpty(message = "At least one test case is required")
  private List<@Valid CreateTestCaseRequest> testCases;
}
