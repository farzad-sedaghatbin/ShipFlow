package com.github.farzadsedaghatbin.shipflow.dto.qa;

import java.util.List;
import lombok.*;

/** Results from validating a test case. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseValidationResult {

  /** Whether the test case passed all validations. */
  private Boolean isValid;

  /** List of validation issues found. */
  private List<String> issues;

  /** Completeness score (0-100). */
  private Integer completenessScore;

  /** Whether the steps are actionable and specific. */
  private Boolean hasActionableSteps;

  /** Whether all required fields are present. */
  private Boolean hasRequiredFields;

  /** Suggested improvements. */
  private List<String> suggestions;
}
