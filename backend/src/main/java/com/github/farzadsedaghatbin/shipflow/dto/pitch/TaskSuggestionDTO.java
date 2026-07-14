package com.github.farzadsedaghatbin.shipflow.dto.pitch;

import com.github.farzadsedaghatbin.shipflow.entity.enums.Discipline;
import com.github.farzadsedaghatbin.shipflow.entity.enums.SuggestionSource;
import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** A single AI-suggested deliverable task for a pitch, not yet created. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSuggestionDTO {

  /** Short, actionable task name (5-10 words). */
  private String title;

  /** What to build, how the listed disciplines collaborate on it, and any design mapping. */
  private String description;

  /** Rough estimate in hours; null when the LLM didn't provide one. */
  private BigDecimal estimateHours;

  /** Whether this suggestion was grounded in pitch text alone or also in Figma design context. */
  private SuggestionSource sourceContext;

  /** Delivery disciplines whose work is needed to call this deliverable done. */
  private List<Discipline> disciplines;
}
