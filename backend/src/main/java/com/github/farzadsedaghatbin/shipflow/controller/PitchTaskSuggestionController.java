package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.service.PitchTaskSuggestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for AI-recommended pitch deliverable tasks.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>{@code POST /api/ai/pitch-task-suggestions/{pitchId}/generate} — generate suggested
 *       deliverable tasks for a pitch via LLM
 *   <li>{@code GET /api/ai/pitch-task-suggestions/status} — probe whether the AI backend is
 *       available
 * </ul>
 */
@RestController
@RequestMapping("/api/ai/pitch-task-suggestions")
@RequiredArgsConstructor
@Tag(name = "AI Pitch Task Suggestions", description = "Generate deliverable task suggestions for a pitch using an LLM")
public class PitchTaskSuggestionController {

  private final PitchTaskSuggestionService pitchTaskSuggestionService;

  /**
   * Generate deliverable task suggestions for a pitch, grounded in the pitch's Shape Up fields
   * and — when configured — Figma design context.
   */
  @PostMapping("/{pitchId}/generate")
  @Operation(
      summary = "Generate deliverable task suggestions for a pitch",
      description =
          "Returns a list of AI-suggested deliverable tasks grounded in the pitch's problem,"
              + " solution, appetite, and (when the org's Figma access token is configured and the"
              + " pitch has a parseable Figma link) Figma design context.")
  @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'EXECUTE')")
  public ResponseEntity<TaskSuggestionResponseDTO> generate(@PathVariable Long pitchId) {
    return ResponseEntity.ok(pitchTaskSuggestionService.suggestTasks(pitchId));
  }

  /**
   * Probe endpoint — returns whether the AI backend is configured and available.
   *
   * <p>No authentication required so that the frontend can conditionally show the AI trigger
   * button without a separate auth check.
   */
  @GetMapping("/status")
  @Operation(
      summary = "Check AI task suggestion availability",
      description =
          "Returns {\"available\": true} when an LLM provider is configured, false otherwise."
              + " No authentication required.")
  @PreAuthorize("permitAll()")
  public ResponseEntity<Map<String, Boolean>> status() {
    return ResponseEntity.ok(Map.of("available", pitchTaskSuggestionService.isAvailable()));
  }
}
