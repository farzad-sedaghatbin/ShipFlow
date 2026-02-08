package com.github.farzadsedaghatbin.shipflow.dto.betting;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import java.util.List;
import java.util.Map;
import lombok.*;

/**
 * DTO for betting decision history and statistics.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BettingDecisionHistoryDTO {

  private Long cycleId;
  private String cycleName;

  /** All decisions for this cycle */
  private List<BettingDecisionDTO> decisions;

  /** Count by decision type */
  private Map<BettingDecisionType, Long> decisionCounts;

  /** Summary statistics */
  private int totalDecisions;
  private int committedCount;
  private int rejectedCount;
  private int deferredCount;
  private int needsShapingCount;

  /** Pitches that have been rejected multiple times (across cycles) */
  private List<RepeatedlyRejectedPitchDTO> repeatedlyRejectedPitches;

  /** Appetite analysis summary */
  private int decisionsOverCapacity;
  private double averageAppetiteDays;
  private double averageAvailableCapacityDays;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class RepeatedlyRejectedPitchDTO {
    private Long pitchId;
    private String pitchTitle;
    private int rejectionCount;
    private List<String> rejectionReasons;
  }
}
