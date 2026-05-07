package com.github.farzadsedaghatbin.shipflow.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.*;

/**
 * Request DTO for converting retrospective action items into pitch drafts.
 * Part of v0.5 - enabling closed retro insights to feed into the next cycle's shaping.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConvertRetroToPitchRequest {
  
  @NotNull(message = "Retrospective ID is required")
  private Long retrospectiveId;

  /**
   * Optional list of specific retro item IDs to include.
   * If null or empty, all action/improvement items will be included.
   */
  private List<Long> retroItemIds;

  /**
   * Target cycle ID for the new pitch draft.
   * If null, will use the project's next upcoming cycle.
   */
  private Long targetCycleId;

  /**
   * Optional custom title for the pitch.
   * If null, will be auto-generated from retro title.
   */
  private String customTitle;

  /**
   * Optional additional context or notes to include.
   */
  private String additionalNotes;

  /**
   * Appetite in days for the generated pitch (default: 1).
   */
  @Builder.Default
  private Integer appetiteDays = 1;
}
