package com.github.farzadsedaghatbin.shipflow.dto.betting;

import java.util.List;
import lombok.*;

/** DTO for "what-if" scenario planning in betting tables */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BettingScenarioDTO {
  private String scenarioName;
  private List<PitchAssignment> assignments;

  // Analysis of this scenario
  private Double totalUtilization; // Percentage
  private Integer totalWeeksUsed;
  private Integer totalWeeksAvailable;
  private List<String> conflicts;
  private List<String> risks;

  // Comparison to other scenarios
  private String viabilityScore; // "HIGH", "MEDIUM", "LOW"
  private List<String> pros;
  private List<String> cons;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PitchAssignment {
    private Long pitchId;
    private String pitchTitle;
    private Long teamId;
    private String teamName;
    private Integer appetiteDays;
  }
}
