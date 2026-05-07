package com.github.farzadsedaghatbin.shipflow.dto.qa;

import java.util.List;
import lombok.*;

/** Request DTO for AI test case generation. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateTestCasesRequest {

  /** Pitch ID to generate test cases for. */
  private Long pitchId;

  /** Additional context or requirements for test generation. */
  private String additionalContext;

  /** Specific features or areas to focus on. */
  private List<String> focusAreas;

  /** Maximum number of test cases to generate. */
  private Integer maxTestCases;

  /** Types of test cases to generate. */
  private List<String> testTypes;
}
