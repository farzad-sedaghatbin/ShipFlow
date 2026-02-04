package com.github.farzadsedaghatbin.shipflow.dto.betting;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Request DTO for recording a betting decision.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordBettingDecisionRequest {

  @NotNull(message = "Pitch ID is required")
  private Long pitchId;

  @NotNull(message = "Cycle ID is required")
  private Long cycleId;

  @NotNull(message = "Decision type is required")
  private BettingDecisionType decision;

  @NotBlank(message = "Reason for decision is required")
  private String reason;

  /** Team being considered for this pitch (optional) */
  private Long consideredTeamId;

  /** Available capacity in days when decision was made (optional) */
  private Integer availableCapacityDays;

  /** Notes about how appetite compares to capacity (optional) */
  private String appetiteComparisonNotes;

  /** Priority score for comparing multiple pitches (optional, 0-100) */
  private Integer priorityScore;

  /** Strategic alignment notes (optional) */
  private String strategicAlignmentNotes;
}
