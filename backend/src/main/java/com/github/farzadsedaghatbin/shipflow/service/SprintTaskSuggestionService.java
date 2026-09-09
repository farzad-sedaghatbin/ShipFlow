package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.entity.enums.SuggestionSource;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that uses an LLM to suggest deliverable tasks for a Scrum sprint, grounded in the
 * cycle's sprint goal, name, and existing backlog — the Scrum equivalent of {@link
 * PitchTaskSuggestionService}.
 *
 * <p>Unlike the pitch version, there is no appetite/rabbit-holes/no-gos/Figma context — those are
 * Shape-Up-only concepts that don't apply to a sprint.
 *
 * <p>Pure generation service — no persistence. Task creation from accepted suggestions happens
 * via {@link TaskService#bulkCreate}.
 */
@Service
@Slf4j
public class SprintTaskSuggestionService {

  /** Cap on existing backlog task titles fed into the prompt, to keep it a reasonable size. */
  private static final int MAX_EXISTING_TASK_TITLES = 15;

  private final ChatLanguageModel chatLanguageModel;
  private final ObjectMapper objectMapper;
  private final CycleRepository cycleRepository;
  private final TaskRepository taskRepository;

  @Autowired
  public SprintTaskSuggestionService(
      @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      ObjectMapper objectMapper,
      CycleRepository cycleRepository,
      TaskRepository taskRepository) {
    this.chatLanguageModel = chatLanguageModel;
    this.objectMapper = objectMapper;
    this.cycleRepository = cycleRepository;
    this.taskRepository = taskRepository;
  }

  /** Returns {@code true} when a {@link ChatLanguageModel} bean is configured and available. */
  public boolean isAvailable() {
    return chatLanguageModel != null;
  }

  /**
   * Generate deliverable task suggestions for a Scrum sprint (cycle).
   *
   * @param cycleId the cycle (sprint) to suggest tasks for
   * @return suggestions; {@code figmaContextUsed} is always {@code false} — Figma is a Shape-Up
   *     pitch concept, not applicable to sprints
   * @throws ResourceNotFoundException if the cycle doesn't exist
   * @throws IllegalStateException if the LLM is not configured or its response can't be parsed
   */
  public TaskSuggestionResponseDTO suggestTasks(Long cycleId) {
    if (chatLanguageModel == null) {
      throw new IllegalStateException(
          "AI task suggestions are not available: no LLM provider is configured."
              + " Please configure app.ai.provider and the corresponding credentials.");
    }

    Cycle cycle = cycleRepository.findById(cycleId)
        .orElseThrow(() -> new ResourceNotFoundException("Cycle not found with id: " + cycleId));

    List<String> existingTaskTitles = taskRepository.findByCycleIdNotDeleted(cycleId).stream()
        .map(Task::getTitle)
        .filter(title -> title != null && !title.isBlank())
        .limit(MAX_EXISTING_TASK_TITLES)
        .collect(Collectors.toList());

    String prompt = buildPrompt(cycle, existingTaskTitles);

    log.info("Generating AI task suggestions for sprint '{}' (cycleId={})", cycle.getName(),
        cycleId);
    long startTime = System.currentTimeMillis();

    String rawResponse = chatLanguageModel.generate(prompt);

    log.info("AI task suggestions received in {}ms", System.currentTimeMillis() - startTime);

    String json = AIPitchWriterService.stripMarkdownFences(rawResponse);

    List<TaskSuggestionDTO> suggestions;
    try {
      suggestions = objectMapper.readValue(json, new TypeReference<List<TaskSuggestionDTO>>() {});
    } catch (Exception e) {
      log.error("Failed to parse LLM task-suggestion response as JSON. Raw response: {}", rawResponse, e);
      throw new IllegalStateException(
          "AI returned a response that could not be parsed. Please try again.", e);
    }

    // Defensive: drop any suggestion the LLM produced with a missing/empty disciplines list,
    // despite the explicit schema — never surface a broken suggestion. Also force sourceContext
    // to SPRINT regardless of what the LLM returned — never trust it to follow instructions.
    suggestions = suggestions.stream()
        .filter(s -> s.getTitle() != null && !s.getTitle().isBlank())
        .filter(s -> s.getDisciplines() != null && !s.getDisciplines().isEmpty())
        .peek(s -> s.setSourceContext(SuggestionSource.SPRINT))
        .collect(Collectors.toList());

    return TaskSuggestionResponseDTO.builder()
        .suggestions(suggestions)
        .figmaContextUsed(false)
        .build();
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private String buildPrompt(Cycle cycle, List<String> existingTaskTitles) {
    StringBuilder sb = new StringBuilder();

    sb.append("You are helping a cross-functional product team break a Scrum sprint into"
        + " concrete deliverable tasks.\n\n");
    sb.append("Each task is delivered collaboratively by the team's disciplines: DESIGN, BACKEND,"
        + " MOBILE (frontend/mobile), and QA. Most deliverables require multiple disciplines"
        + " working together to actually ship (e.g. an API + the screen that calls it + a test"
        + " pass). Only mark a task single-discipline when it truly has no dependency on the"
        + " others (e.g. a pure backend migration script, or a pure QA test-plan task with"
        + " nothing new to build).\n\n");

    sb.append("SPRINT CONTEXT:\n");
    sb.append("Sprint Name: ").append(cycle.getName()).append("\n");
    String sprintGoal = cycle.getSprintGoal();
    if (sprintGoal != null && !sprintGoal.isBlank()) {
      sb.append("Sprint Goal: ").append(sprintGoal).append("\n");
    } else {
      sb.append("Sprint Goal: (none set — infer scope conservatively from the sprint name and"
          + " existing backlog below)\n");
    }
    if (cycle.getProject() != null && cycle.getProject().getName() != null) {
      sb.append("Project: ").append(cycle.getProject().getName()).append("\n");
    }

    if (!existingTaskTitles.isEmpty()) {
      sb.append("\nEXISTING TASKS ALREADY IN THIS SPRINT (do not suggest obvious duplicates of"
          + " these):\n");
      for (String title : existingTaskTitles) {
        sb.append("- ").append(title).append("\n");
      }
    }

    sb.append("\nINSTRUCTIONS:\n");
    sb.append("- Suggest 4-8 concrete, actionable deliverable tasks that implement this sprint's"
        + " goal.\n");
    sb.append("- Every task MUST be grounded in the SPRINT CONTEXT above — do not invent scope"
        + " unrelated to the sprint goal or name.\n");
    sb.append("- Do NOT suggest tasks that duplicate an existing task listed above.\n");
    sb.append("- For each task, list every discipline (\"DESIGN\", \"BACKEND\", \"MOBILE\", \"QA\")"
        + " whose work is genuinely needed to call that deliverable done. Default to"
        + " multi-discipline lists; use a single discipline only when justified.\n");
    sb.append("- Use \"sourceContext\": \"SPRINT\" for every task — this is a Scrum sprint, not a"
        + " Shape Up pitch.\n");

    sb.append("\nReturn ONLY a raw JSON array (no markdown fences, no prose) matching EXACTLY this"
        + " schema, one object per task:\n");
    sb.append("[\n");
    sb.append("  {\n");
    sb.append("    \"title\": \"Short, actionable task name (5-10 words)\",\n");
    sb.append("    \"description\": \"2-4 sentences: what to build and how the listed disciplines"
        + " collaborate on it\",\n");
    sb.append("    \"estimateHours\": 12,\n");
    sb.append("    \"sourceContext\": \"SPRINT\",\n");
    sb.append("    \"disciplines\": [\"BACKEND\", \"MOBILE\", \"QA\"]\n");
    sb.append("  }\n");
    sb.append("]\n\n");
    sb.append("Rules:\n");
    sb.append("- \"sourceContext\" must be exactly \"SPRINT\".\n");
    sb.append("- \"disciplines\" must be a non-empty array using only \"DESIGN\", \"BACKEND\","
        + " \"MOBILE\", \"QA\".\n");
    sb.append("- \"estimateHours\" is a rough number (integer or decimal); omit the field (do not"
        + " use null) if truly unknown.\n");
    sb.append("- Return ONLY the raw JSON array, no markdown fences, no explanation");

    return sb.toString();
  }
}
