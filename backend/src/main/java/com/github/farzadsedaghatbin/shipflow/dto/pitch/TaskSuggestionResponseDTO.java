package com.github.farzadsedaghatbin.shipflow.dto.pitch;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Response returned by {@code POST /api/ai/pitch-task-suggestions/{pitchId}/generate}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSuggestionResponseDTO {

  private List<TaskSuggestionDTO> suggestions;

  /**
   * Whether Figma design context was actually fetched and fed into the prompt. False whenever the
   * org has no Figma token, the pitch has no parseable Figma link, or the Figma call returned no
   * content — the suggestions are still generated pitch-only in that case.
   */
  private boolean figmaContextUsed;
}
