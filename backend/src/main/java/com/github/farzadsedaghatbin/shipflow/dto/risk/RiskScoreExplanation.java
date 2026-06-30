package com.github.farzadsedaghatbin.shipflow.dto.risk;

import com.github.farzadsedaghatbin.shipflow.entity.enums.RiskLevel;
import java.util.List;
import lombok.*;

/**
 * Structured, human-legible explanation of how a risk score maps to a risk
 * level. Surfaced at the controller boundary so the UI can show WHY a score
 * lands in a given band and how each risk factor contributes to it.
 *
 * <p>
 * All numbers here are derived from the real scoring pipeline (configurable
 * {@code RiskThresholds} for the bands, and the {@code impactLevel * probability
 * / 10} weighting used by {@code calculateBaseRiskScore} for factor
 * contributions) — nothing is fabricated.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScoreExplanation {

  /** The score being explained (0-100), mirrored from the parent DTO. */
  private Integer score;

  /** The risk band the score currently falls into. */
  private RiskLevel activeBand;

  /**
   * The full threshold legend: the four risk bands with their numeric ranges.
   * Ordered LOW → CRITICAL. Exactly one band has {@code active = true}.
   */
  private List<RiskBand> bands;

  /**
   * Per-factor contribution to the score, derived from each factor's impact and
   * probability. May be empty when no discrete factors were identified (the
   * score then comes from the baseline progress-vs-cycle calculation).
   */
  private List<FactorContribution> factorContributions;

  /** A single band in the threshold legend. */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RiskBand {

    /** The risk level this band represents. */
    private RiskLevel level;

    /** Inclusive lower bound of the band's score range (0-100). */
    private Integer minScore;

    /** Inclusive upper bound of the band's score range (0-100). */
    private Integer maxScore;

    /** Whether the explained score falls inside this band. */
    private boolean active;
  }

  /**
   * The contribution of a single risk factor to the overall score. The weighted
   * points mirror the {@code impactLevel * probability / 10} term summed in
   * {@code calculateBaseRiskScore}.
   */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class FactorContribution {

    /** The risk factor category (e.g. TIME_OVERRUN). */
    private RiskFactor.RiskCategory category;

    /** Human-readable description of the specific risk. */
    private String description;

    /** Impact level of this factor (1-10). */
    private Integer impactLevel;

    /** Probability of this risk materializing (1-10). */
    private Integer probability;

    /**
     * Weighted points this factor contributes to the raw factor sum, i.e.
     * {@code impactLevel * probability / 10.0}, rounded to one decimal. Higher =
     * more of the score comes from this factor.
     */
    private Double weightedPoints;
  }
}
