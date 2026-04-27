package com.github.farzadsedaghatbin.shipflow.dto.betting;

import com.github.farzadsedaghatbin.shipflow.dto.PitchDTO;
import com.github.farzadsedaghatbin.shipflow.entity.enums.BusinessValue;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ComplexityLevel;
import com.github.farzadsedaghatbin.shipflow.entity.enums.Urgency;
import java.util.List;
import lombok.*;

/** DTO for pitch comparison view with risk and capacity analysis */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PitchComparisonDTO {
  private PitchDTO pitch;

  // Risk indicators
  private Integer appetiteDays;
  private ComplexityLevel complexityLevel;
  private List<String> risks;
  private List<String> rabbitHoles;

  // Team fit analysis
  private List<TeamFitAnalysis> teamFitScores;

  // Estimated impact
  private BusinessValue estimatedBusinessValue;
  private Urgency urgency;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class TeamFitAnalysis {
    private Long teamId;
    private String teamName;
    private Integer availableCapacityWeeks;
    private Boolean canFit;
    private String capacityStatus; // "AVAILABLE", "TIGHT", "OVER_ALLOCATED"
    private String recommendation; // "GOOD_FIT", "POSSIBLE_WITH_TRADEOFFS", "NOT_RECOMMENDED"
    private List<String> warnings;
  }
}
