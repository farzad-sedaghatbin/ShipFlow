package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.*;

/** DTO for test coverage statistics for a pitch. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCoverageDTO {
  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;

  // Test case statistics
  private Long totalTestCases;
  private Long draftTestCases;
  private Long readyTestCases;
  private Long approvedTestCases;

  // Test run statistics
  private Long totalRuns;
  private Long passedRuns;
  private Long failedRuns;
  private Long blockedRuns;
  private Long skippedRuns;

  // Coverage metrics
  private Double testCaseCoverage; // % of test cases that have been run
  private Double passRate; // % of runs that passed
  private Double coverageScore; // Overall coverage score (0-100)

  // Bug statistics
  private Long totalBugs;
  private Long openBugs;
  private Long criticalBugs;
  private Long blockerBugs;
}
