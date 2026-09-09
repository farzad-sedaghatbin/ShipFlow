package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.service.SprintTaskSuggestionService;
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
 * REST controller for AI-recommended Scrum sprint deliverable tasks.
 *
 * <p>Endpoints:
 *
 * <ul>
 *   <li>{@code POST /api/ai/sprint-task-suggestions/{cycleId}/generate} — generate suggested
 *       deliverable tasks for a sprint (cycle) via LLM
 *   <li>{@code GET /api/ai/sprint-task-suggestions/status} — probe whether the AI backend is
 *       available
 * </ul>
 */
@RestController
@RequestMapping("/api/ai/sprint-task-suggestions")
@RequiredArgsConstructor
@Tag(name = "AI Sprint Task Suggestions", description = "Generate deliverable task suggestions for a Scrum sprint using an LLM")
public class SprintTaskSuggestionController {

  private final SprintTaskSuggestionService sprintTaskSuggestionService;

  /**
   * Generate deliverable task suggestions for a sprint, grounded in the cycle's sprint goal, name,
   * and existing backlog.
   */
  @PostMapping("/{cycleId}/generate")
  @Operation(
      summary = "Generate deliverable task suggestions for a sprint",
      description =
          "Returns a list of AI-suggested deliverable tasks grounded in the sprint's goal, name,"
              + " and existing backlog tasks already in the sprint.")
  @PreAuthorize("@permissionService.hasPermission('AI_FEATURES', 'EXECUTE')")
  public ResponseEntity<TaskSuggestionResponseDTO> generate(@PathVariable Long cycleId) {
    return ResponseEntity.ok(sprintTaskSuggestionService.suggestTasks(cycleId));
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
    return ResponseEntity.ok(Map.of("available", sprintTaskSuggestionService.isAvailable()));
  }
}
