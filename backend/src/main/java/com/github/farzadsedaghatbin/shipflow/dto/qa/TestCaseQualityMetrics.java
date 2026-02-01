package com.github.farzadsedaghatbin.shipflow.dto.qa;

import lombok.*;

/** Quality metrics for generated test cases. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseQualityMetrics {

  /** Percentage of acceptance criteria covered (0-100). */
  private Double requirementCoverage;

  /** Diversity of test scenarios (0-100). */
  private Double scenarioDiversity;

  /** Clarity and specificity of test steps (0-100). */
  private Double stepClarity;

  /** How actionable the test steps are (0-100). */
  private Double actionability;

  /** Consistency with historical test patterns (0-100). */
  private Double consistency;

  /** Whether all components are present (0-100). */
  private Double completeness;

  /** Overall quality score (0-100). */
  private Double overallScore;

  /** Calculated overall score from individual metrics. */
  public Double calculateOverallScore() {
    double sum = 0;
    int count = 0;

    if (requirementCoverage != null) {
      sum += requirementCoverage;
      count++;
    }
    if (scenarioDiversity != null) {
      sum += scenarioDiversity;
      count++;
    }
    if (stepClarity != null) {
      sum += stepClarity;
      count++;
    }
    if (actionability != null) {
      sum += actionability;
      count++;
    }
    if (consistency != null) {
      sum += consistency;
      count++;
    }
    if (completeness != null) {
      sum += completeness;
      count++;
    }

    return count > 0 ? sum / count : 0.0;
  }
}
