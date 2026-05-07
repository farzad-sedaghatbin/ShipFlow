package com.github.farzadsedaghatbin.shipflow.dto.signal;

import java.util.List;
import lombok.*;

/**
 * Signal DTO for risk-outcome correlation analysis.
 * Shows how well early risk detection predicted actual outcomes.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskCorrelationSignalDTO {

  /** How often high-risk predictions were accurate */
  private Double predictiveAccuracyPercent;

  /** Number of pitches analyzed */
  private Integer pitchesAnalyzed;

  /** Pitches flagged as high risk that actually had problems */
  private Integer truePositives;

  /** Pitches flagged as high risk that completed fine */
  private Integer falseAlarms;

  /** Pitches NOT flagged that had problems */
  private Integer missedRisks;

  /** Pitches correctly identified as low risk */
  private Integer trueNegatives;

  /** Details on missed risks - things we should have caught */
  private List<MissedRiskPitch> missedRiskDetails;

  /** Details on false alarms - over-flagged risks */
  private List<FalseAlarmPitch> falseAlarmDetails;

  /** Risk factors that proved most predictive */
  private List<String> mostPredictiveFactors;

  /** Risk factors that often led to false alarms */
  private List<String> leastPredictiveFactors;

  /** Plain-language interpretation */
  private String interpretation;

  /** Recommendations for improving risk detection */
  private List<String> recommendations;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class MissedRiskPitch {
    private Long pitchId;
    private String pitchTitle;
    private String cycleName;
    private String initialRiskLevel;
    private String finalOutcome;
    private String whatWentWrong;
    private List<String> warningSignsMissed;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class FalseAlarmPitch {
    private Long pitchId;
    private String pitchTitle;
    private String cycleName;
    private String flaggedRiskLevel;
    private List<String> flaggedRisks;
    private String actualOutcome;
  }
}
