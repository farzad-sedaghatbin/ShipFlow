package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionDTO;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.TaskSuggestionResponseDTO;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.service.mcp.FigmaMcpProvider;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that uses an LLM to suggest deliverable tasks for a pitch, grounded in the pitch's
 * Shape Up fields and — when the org has a Figma access token configured and the pitch's
 * wireframe links contain a parseable Figma URL — Figma design context as well.
 *
 * <p>Pure generation service — no persistence. Task creation from accepted suggestions happens
 * via {@link TaskService#bulkCreate}.
 */
@Service
@Slf4j
public class PitchTaskSuggestionService {

  private final ChatLanguageModel chatLanguageModel;
  private final ObjectMapper objectMapper;
  private final PitchRepository pitchRepository;
  private final OrganizationSettingsService settingsService;
  private final FigmaMcpProvider figmaMcpProvider;

  @Autowired
  public PitchTaskSuggestionService(
      @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      ObjectMapper objectMapper,
      PitchRepository pitchRepository,
      OrganizationSettingsService settingsService,
      FigmaMcpProvider figmaMcpProvider) {
    this.chatLanguageModel = chatLanguageModel;
    this.objectMapper = objectMapper;
    this.pitchRepository = pitchRepository;
    this.settingsService = settingsService;
    this.figmaMcpProvider = figmaMcpProvider;
  }

  /** Returns {@code true} when a {@link ChatLanguageModel} bean is configured and available. */
  public boolean isAvailable() {
    return chatLanguageModel != null;
  }

  /**
   * Generate deliverable task suggestions for a pitch.
   *
   * @param pitchId the pitch to suggest tasks for
   * @return suggestions plus whether Figma design context was actually used
   * @throws ResourceNotFoundException if the pitch doesn't exist
   * @throws IllegalStateException if the LLM is not configured or its response can't be parsed
   */
  public TaskSuggestionResponseDTO suggestTasks(Long pitchId) {
    if (chatLanguageModel == null) {
      throw new IllegalStateException(
          "AI task suggestions are not available: no LLM provider is configured."
              + " Please configure app.ai.provider and the corresponding credentials.");
    }

    Pitch pitch = pitchRepository.findById(pitchId)
        .orElseThrow(() -> new ResourceNotFoundException("Pitch not found with id: " + pitchId));

    String figmaContext = extractFigmaContext(pitch);
    boolean figmaContextUsed = figmaContext != null && !figmaContext.isBlank();

    String prompt = buildPrompt(pitch, figmaContext);

    log.info("Generating AI task suggestions for pitch '{}' (figmaContextUsed={})",
        pitch.getTitle(), figmaContextUsed);
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

    // Defensive: drop any suggestion the LLM produced with a missing/empty disciplines list or an
    // invalid sourceContext, despite the explicit schema — never surface a broken suggestion.
    suggestions = suggestions.stream()
        .filter(s -> s.getTitle() != null && !s.getTitle().isBlank())
        .filter(s -> s.getDisciplines() != null && !s.getDisciplines().isEmpty())
        .filter(s -> s.getSourceContext() != null)
        .collect(Collectors.toList());

    return TaskSuggestionResponseDTO.builder()
        .suggestions(suggestions)
        .figmaContextUsed(figmaContextUsed)
        .build();
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  /**
   * Extract Figma design context from the pitch's wireframe links, exactly mirroring {@code
   * WiseArchitectureService.extractFigmaContext}: null-guard wireframe links, parse Figma file
   * references, guard on the org's Figma access token, fetch design context, guard on content.
   * Returns null (never throws) whenever any guard fails so the caller can degrade gracefully to
   * a pitch-only prompt.
   */
  private String extractFigmaContext(Pitch pitch) {
    String wireframeLinks = pitch.getWireframeLinks();
    if (wireframeLinks == null || wireframeLinks.isBlank()) {
      return null;
    }

    List<FigmaMcpProvider.FigmaFileReference> figmaRefs =
        figmaMcpProvider.extractFigmaFileReferences(wireframeLinks);
    if (figmaRefs.isEmpty()) {
      return null;
    }

    String figmaToken = settingsService.getFigmaAccessToken();
    if (figmaToken == null || figmaToken.isBlank()) {
      return null;
    }

    FigmaMcpProvider.FigmaFileReference fileRef = figmaRefs.get(0);
    FigmaMcpProvider.FigmaDesignContext context = figmaMcpProvider.getDesignContext(fileRef, figmaToken);

    if (context == null || !context.hasContent()) {
      return null;
    }

    return context.toPromptString();
  }

  private String buildPrompt(Pitch pitch, String figmaContext) {
    StringBuilder sb = new StringBuilder();

    sb.append("You are helping a cross-functional product team break a Shape Up pitch into"
        + " concrete deliverable tasks.\n\n");
    sb.append("Each task becomes a \"scope\" tracked on a hill chart and is delivered"
        + " collaboratively by the team's disciplines: DESIGN, BACKEND, MOBILE (frontend/mobile),"
        + " and QA. Most deliverables require multiple disciplines working together to actually"
        + " ship (e.g. an API + the screen that calls it + a test pass). Only mark a task"
        + " single-discipline when it truly has no dependency on the others (e.g. a pure backend"
        + " migration script, or a pure QA test-plan task with nothing new to build).\n\n");

    sb.append("PITCH CONTEXT:\n");
    sb.append("Title: ").append(pitch.getTitle()).append("\n");
    sb.append("Problem Statement: ").append(nullToEmpty(pitch.getProblemStatement())).append("\n");
    sb.append("Solution: ").append(nullToEmpty(pitch.getSolution())).append("\n");
    sb.append("Appetite: ").append(pitch.getAppetiteDays()).append(" days\n");
    sb.append("Rabbit Holes: ").append(nullToEmpty(pitch.getRabbitHoles())).append("\n");
    sb.append("Risks: ").append(nullToEmpty(pitch.getRisks())).append("\n");
    sb.append("No-Gos: ").append(nullToEmpty(pitch.getNoGos())).append("\n");

    if (figmaContext != null && !figmaContext.isBlank()) {
      sb.append("\nDESIGN CONTEXT (from Figma):\n").append(figmaContext).append("\n");
    }

    sb.append("\nINSTRUCTIONS:\n");
    sb.append("- Suggest 4-8 concrete, actionable deliverable tasks that implement this pitch.\n");
    sb.append("- Every task MUST be grounded in the PITCH CONTEXT above — do not invent scope"
        + " outside the problem/solution/appetite.\n");
    sb.append("- For each task, list every discipline (\"DESIGN\", \"BACKEND\", \"MOBILE\", \"QA\")"
        + " whose work is genuinely needed to call that deliverable done. Default to"
        + " multi-discipline lists; use a single discipline only when justified.\n");
    if (figmaContext != null && !figmaContext.isBlank()) {
      sb.append("- Any task whose disciplines include \"DESIGN\" or \"MOBILE\" MUST reference"
          + " concrete details from DESIGN CONTEXT (component names, layout, states) and use"
          + " \"sourceContext\": \"PITCH_DESIGN\". Tasks that only draw on the pitch text use"
          + " \"sourceContext\": \"PITCH\".\n");
    } else {
      sb.append("- No design context is available — use \"sourceContext\": \"PITCH\" for every"
          + " task.\n");
    }
    sb.append("- Do NOT suggest tasks that fall in Rabbit Holes or No-Gos.\n");
    sb.append("- Respect the appetite — do not suggest scope that clearly exceeds ")
        .append(pitch.getAppetiteDays()).append(" days total.\n");

    sb.append("\nReturn ONLY a raw JSON array (no markdown fences, no prose) matching EXACTLY this"
        + " schema, one object per task:\n");
    sb.append("[\n");
    sb.append("  {\n");
    sb.append("    \"title\": \"Short, actionable task name (5-10 words)\",\n");
    sb.append("    \"description\": \"2-4 sentences: what to build, how the listed disciplines"
        + " collaborate on it, and — if design-influenced — which UI element it maps to\",\n");
    sb.append("    \"estimateHours\": 12,\n");
    sb.append("    \"sourceContext\": \"PITCH\",\n");
    sb.append("    \"disciplines\": [\"BACKEND\", \"MOBILE\", \"QA\"]\n");
    sb.append("  }\n");
    sb.append("]\n\n");
    sb.append("Rules:\n");
    sb.append("- \"sourceContext\" must be exactly \"PITCH\" or \"PITCH_DESIGN\".\n");
    sb.append("- \"disciplines\" must be a non-empty array using only \"DESIGN\", \"BACKEND\","
        + " \"MOBILE\", \"QA\".\n");
    sb.append("- \"estimateHours\" is a rough number (integer or decimal); omit the field (do not"
        + " use null) if truly unknown.\n");
    sb.append("- Return ONLY the raw JSON array, no markdown fences, no explanation");

    return sb.toString();
  }

  private String nullToEmpty(String s) {
    return s == null ? "" : s;
  }
}
