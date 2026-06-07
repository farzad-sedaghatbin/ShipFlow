package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.PitchWriterRequestDTO;
import com.github.farzadsedaghatbin.shipflow.dto.pitch.PitchWriterResponseDTO;
import dev.langchain4j.model.chat.ChatLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service that uses an LLM to generate a fully structured Shape Up pitch draft from a problem
 * description. No persistence — this is a pure generation service.
 */
@Service
@Slf4j
public class AIPitchWriterService {

  private final ChatLanguageModel chatLanguageModel;
  private final ObjectMapper objectMapper;

  @Autowired
  public AIPitchWriterService(
      @Autowired(required = false) ChatLanguageModel chatLanguageModel, ObjectMapper objectMapper) {
    this.chatLanguageModel = chatLanguageModel;
    this.objectMapper = objectMapper;
  }

  /** Returns {@code true} when a {@link ChatLanguageModel} bean is configured and available. */
  public boolean isAvailable() {
    return chatLanguageModel != null;
  }

  /**
   * Generate a Shape Up pitch draft from the supplied request.
   *
   * @param request contains the problem description and optional appetite / context hints
   * @return a structured {@link PitchWriterResponseDTO}
   * @throws IllegalStateException if the LLM is not configured
   * @throws IllegalStateException if the LLM response cannot be parsed as JSON
   */
  public PitchWriterResponseDTO generatePitch(PitchWriterRequestDTO request) {
    if (chatLanguageModel == null) {
      throw new IllegalStateException(
          "AI Pitch Writer is not available: no LLM provider is configured."
              + " Please configure app.ai.provider and the corresponding credentials.");
    }

    String prompt = buildPrompt(request);

    log.info("Generating AI pitch draft for problem: {}", truncate(request.getProblemDescription(), 80));
    long startTime = System.currentTimeMillis();

    String rawResponse = chatLanguageModel.generate(prompt);

    log.info("AI pitch draft received in {}ms", System.currentTimeMillis() - startTime);

    String json = stripMarkdownFences(rawResponse);

    try {
      return objectMapper.readValue(json, PitchWriterResponseDTO.class);
    } catch (Exception e) {
      log.error("Failed to parse LLM pitch response as JSON. Raw response: {}", rawResponse, e);
      throw new IllegalStateException(
          "AI returned a response that could not be parsed. Please try again.", e);
    }
  }

  // ── Private helpers ──────────────────────────────────────────────────────────

  private String buildPrompt(PitchWriterRequestDTO request) {
    StringBuilder sb = new StringBuilder();
    sb.append(
        "You are ShipFlow's AI Pitch Writer, an expert in the Shape Up methodology by Basecamp.\n\n");
    sb.append("The user wants to write a Shape Up pitch for a software feature.\n\n");
    sb.append("Problem description: \"").append(request.getProblemDescription()).append("\"\n");

    if (request.getAppetiteHint() != null) {
      sb.append("Preferred appetite: ").append(request.getAppetiteHint()).append(" days\n");
    }
    if (request.getProjectContext() != null && !request.getProjectContext().isBlank()) {
      sb.append("Project context: ").append(request.getProjectContext()).append("\n");
    }

    sb.append("\nGenerate a JSON object with EXACTLY these fields:\n");
    sb.append("{\n");
    sb.append("  \"title\": \"Short memorable title (5-10 words)\",\n");
    sb.append(
        "  \"problemStatement\": \"Why this problem matters and who it affects (2-3 paragraphs)\",\n");
    sb.append(
        "  \"solution\": \"What to build at the right level of abstraction — not implementation details (2-3 paragraphs)\",\n");
    sb.append("  \"appetiteDays\": 14,\n");
    sb.append(
        "  \"rabbitHoles\": \"Specific pitfalls the team should avoid (use bullet points with -)\",\n");
    sb.append("  \"noGos\": \"Explicit out-of-scope items (use bullet points with -)\",\n");
    sb.append("  \"risks\": \"Known technical or business risks (use bullet points with -)\"\n");
    sb.append("}\n\n");
    sb.append("Rules:\n");
    sb.append(
        "- appetiteDays must be either 14 (small batch, 2-week cycle) or 28 (big batch, 4-week cycle)\n");
    sb.append("- Return ONLY the raw JSON object, no markdown fences, no explanation");

    return sb.toString();
  }

  /**
   * Remove optional markdown code fences that some LLMs wrap around JSON, e.g.:
   *
   * <pre>
   * ```json
   * { ... }
   * ```
   * </pre>
   */
  static String stripMarkdownFences(String response) {
    if (response == null) {
      return "";
    }
    String trimmed = response.trim();
    // Strip leading ```json or ``` fence
    if (trimmed.startsWith("```")) {
      int firstNewline = trimmed.indexOf('\n');
      if (firstNewline != -1) {
        trimmed = trimmed.substring(firstNewline + 1).trim();
      }
    }
    // Strip trailing ``` fence
    if (trimmed.endsWith("```")) {
      trimmed = trimmed.substring(0, trimmed.lastIndexOf("```")).trim();
    }
    return trimmed;
  }

  private String truncate(String text, int max) {
    if (text == null || text.length() <= max) {
      return text;
    }
    return text.substring(0, max) + "...";
  }
}
