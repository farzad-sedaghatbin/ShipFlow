package com.github.farzadsedaghatbin.shipflow.dto.betting;

import lombok.*;

/**
 * DTO for appetite comparison analysis between a pitch and team capacity.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppetiteComparisonDTO {

  private Long pitchId;
  private String pitchTitle;
  private Integer requestedAppetiteDays;

  private Long teamId;
  private String teamName;
  private Integer availableCapacityDays;

  /** Does the appetite fit within available capacity? */
  private Boolean fits;

  /** Difference in days (positive = excess capacity, negative = over capacity) */
  private Integer capacityDifferenceDays;

  /** Utilization percentage (appetite / capacity * 100) */
  private Double utilizationPercentage;

  /** Fit assessment */
  private FitAssessment assessment;

  /** Human-readable recommendation */
  private String recommendation;

  public enum FitAssessment {
    /** Fits comfortably with room to spare (< 70% utilization) */
    COMFORTABLE,
    /** Fits well (70-90% utilization) */
    GOOD_FIT,
    /** Fits but tight (90-100% utilization) */
    TIGHT_FIT,
    /** Slightly over capacity (100-120%) - may work with scope trimming */
    NEEDS_SCOPE_TRIM,
    /** Significantly over capacity (> 120%) - won't fit */
    WONT_FIT
  }

  /**
   * Calculate fit assessment based on utilization.
   */
  public static FitAssessment calculateAssessment(double utilizationPercentage) {
    if (utilizationPercentage < 70) {
      return FitAssessment.COMFORTABLE;
    } else if (utilizationPercentage < 90) {
      return FitAssessment.GOOD_FIT;
    } else if (utilizationPercentage <= 100) {
      return FitAssessment.TIGHT_FIT;
    } else if (utilizationPercentage <= 120) {
      return FitAssessment.NEEDS_SCOPE_TRIM;
    } else {
      return FitAssessment.WONT_FIT;
    }
  }

  /**
   * Generate recommendation text based on assessment.
   */
  public static String generateRecommendation(FitAssessment assessment, int appetiteDays, int capacityDays) {
    return switch (assessment) {
      case COMFORTABLE -> String.format(
          "Excellent fit. %d days appetite fits comfortably in %d days capacity with room for unexpected work.",
          appetiteDays, capacityDays);
      case GOOD_FIT -> String.format(
          "Good fit. %d days appetite aligns well with %d days capacity.",
          appetiteDays, capacityDays);
      case TIGHT_FIT -> String.format(
          "Tight fit. %d days appetite uses most of %d days capacity. Consider if there's buffer for unknowns.",
          appetiteDays, capacityDays);
      case NEEDS_SCOPE_TRIM -> String.format(
          "Over capacity by %d days. May work if scope can be trimmed or nice-to-haves cut.",
          appetiteDays - capacityDays);
      case WONT_FIT -> String.format(
          "Won't fit. %d days appetite significantly exceeds %d days capacity. Consider deferring or re-shaping.",
          appetiteDays, capacityDays);
    };
  }
}
