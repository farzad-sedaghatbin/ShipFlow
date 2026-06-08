package com.github.farzadsedaghatbin.shipflow.dto.pitch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response DTO returned by the AI Pitch Writer after generating a Shape Up pitch draft. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PitchWriterResponseDTO {

  /** Short memorable title (5-10 words). */
  private String title;

  /** Why this problem matters and who it affects (2-3 paragraphs). */
  private String problemStatement;

  /** What to build at the right level of abstraction (2-3 paragraphs). */
  private String solution;

  /** Appetite in days — either 14 (small batch) or 28 (big batch). */
  private Integer appetiteDays;

  /** Specific pitfalls the team should avoid. */
  private String rabbitHoles;

  /** Explicit out-of-scope items. */
  private String noGos;

  /** Known technical or business risks. */
  private String risks;
}
