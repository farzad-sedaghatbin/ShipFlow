package com.github.farzadsedaghatbin.shipflow.dto.risk;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

/** DTO for cycle-level risk overview aggregating all pitch risks. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CycleRiskOverviewDTO {

  private Long cycleId;
  private String cycleName;
  private String projectName;

  /** Overall cycle risk score (0-100), aggregated from all pitches. */
  private Integer overallRiskScore;

  /** Overall risk level for the cycle. */
  private RiskLevel overallRiskLevel;

  /** Total number of pitches in the cycle. */
  private Integer totalPitches;

  /** Number of high-risk pitches. */
  private Integer highRiskPitches;

  /** Number of medium-risk pitches. */
  private Integer mediumRiskPitches;

  /** Number of low-risk pitches. */
  private Integer lowRiskPitches;

  /** Summary statistics for the cycle. */
  private CycleRiskStats stats;

  /** Individual pitch risk summaries. */
  private List<PitchRiskSummary> pitchRisks;

  /** AI-generated overall insights for the cycle. */
  private List<String> cycleInsights;

  /** AI-generated recommendations for the cycle. */
  private List<String> cycleRecommendations;

  /** Most common risk factors across all pitches. */
  private List<CommonRiskFactor> topRiskFactors;

  /** Timestamp when the analysis was performed. */
  private LocalDateTime analyzedAt;

  /** Whether AI feature is enabled. */
  private boolean aiEnabled;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CycleRiskStats {
    private Double averageRiskScore;
    private Integer maxRiskScore;
    private Integer minRiskScore;
    private Double averageProgress;
    private Double appetiteUtilization;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PitchRiskSummary {
    private Long pitchId;
    private String pitchTitle;
    private Integer riskScore;
    private RiskLevel riskLevel;
    private String topRisk;
    private Double progressPercentage;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class CommonRiskFactor {
    private RiskFactor.RiskCategory category;
    private Integer occurrenceCount;
    private Double averageImpact;
  }
}
