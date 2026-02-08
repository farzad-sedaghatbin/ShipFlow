package com.github.farzadsedaghatbin.shipflow.dto.betting;

import com.github.farzadsedaghatbin.shipflow.entity.enums.BettingDecisionType;
import java.time.LocalDateTime;
import lombok.*;

/**
 * DTO for betting decision response and history display.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BettingDecisionDTO {

  private Long id;
  private Long pitchId;
  private String pitchTitle;
  private Long cycleId;
  private String cycleName;
  
  private BettingDecisionType decision;
  private String reason;
  
  private Long decidedById;
  private String decidedByUsername;
  private LocalDateTime decidedAt;

  // Appetite comparison
  private Integer requestedAppetiteDays;
  private Integer availableCapacityDays;
  private String appetiteComparisonNotes;
  private Boolean appetiteFitsCapacity;

  // Team consideration
  private Long consideredTeamId;
  private String consideredTeamName;

  // Priority and alignment
  private Integer priorityScore;
  private String strategicAlignmentNotes;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
