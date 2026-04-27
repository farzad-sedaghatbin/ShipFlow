package com.github.farzadsedaghatbin.shipflow.dto.cycle;

import com.github.farzadsedaghatbin.shipflow.entity.enums.CyclePhase;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

/**
 * DTO representing the result of a phase transition request.
 * Includes warnings if prerequisites aren't met (transition proceeds with warnings).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PhaseTransitionResultDTO {

  private Long cycleId;
  private String cycleName;
  private CyclePhase previousPhase;
  private CyclePhase newPhase;
  private boolean transitionCompleted;

  /** Warnings about prerequisites that weren't met (transition still allowed) */
  @Builder.Default
  private List<PhaseTransitionWarning> warnings = new ArrayList<>();

  /** Blocking issues that prevented transition (only if transitionCompleted is false) */
  @Builder.Default
  private List<PhaseTransitionWarning> blockers = new ArrayList<>();

  public boolean hasWarnings() {
    return warnings != null && !warnings.isEmpty();
  }

  public boolean hasBlockers() {
    return blockers != null && !blockers.isEmpty();
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class PhaseTransitionWarning {
    private String code;
    private String message;
    private WarningCategory category;
    private int affectedCount; // e.g., number of pitches without decisions
  }

  public enum WarningCategory {
    BETTING_INCOMPLETE,     // Pitches without decisions at BETTING → BUILD
    UNCOMMITTED_PITCHES,    // Shaped pitches not committed
    NO_RETROSPECTIVE,       // No retro at BUILD → COOLDOWN
    PITCHES_NOT_COMPLETE,   // Uncommitted work at cycle end
    EMPTY_CYCLE,            // No pitches shaped
    SKIPPED_PHASE           // Phase order violation (non-sequential)
  }

  public static PhaseTransitionResultDTO success(
      Long cycleId, String cycleName, CyclePhase previousPhase, CyclePhase newPhase) {
    return PhaseTransitionResultDTO.builder()
        .cycleId(cycleId)
        .cycleName(cycleName)
        .previousPhase(previousPhase)
        .newPhase(newPhase)
        .transitionCompleted(true)
        .build();
  }

  public static PhaseTransitionResultDTO successWithWarnings(
      Long cycleId, String cycleName, CyclePhase previousPhase, CyclePhase newPhase, 
      List<PhaseTransitionWarning> warnings) {
    return PhaseTransitionResultDTO.builder()
        .cycleId(cycleId)
        .cycleName(cycleName)
        .previousPhase(previousPhase)
        .newPhase(newPhase)
        .transitionCompleted(true)
        .warnings(warnings)
        .build();
  }

  public static PhaseTransitionResultDTO blocked(
      Long cycleId, String cycleName, CyclePhase currentPhase, CyclePhase attemptedPhase,
      List<PhaseTransitionWarning> blockers) {
    return PhaseTransitionResultDTO.builder()
        .cycleId(cycleId)
        .cycleName(cycleName)
        .previousPhase(currentPhase)
        .newPhase(currentPhase) // Stays at current
        .transitionCompleted(false)
        .blockers(blockers)
        .build();
  }
}
