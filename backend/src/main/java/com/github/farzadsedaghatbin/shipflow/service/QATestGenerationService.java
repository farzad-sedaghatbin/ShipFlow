package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.*;
import com.github.farzadsedaghatbin.shipflow.entity.enums.TestType;
import com.github.farzadsedaghatbin.shipflow.repository.*;
import com.github.farzadsedaghatbin.shipflow.service.qa.TestCasePromptBuilder;
import com.github.farzadsedaghatbin.shipflow.service.qa.TestCaseValidator;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for AI-assisted test case generation using RAG. */
@Service
@Slf4j
public class QATestGenerationService {

  @Value("${app.qa.test-management.enabled:true}")
  private boolean testManagementEnabled;

  @Value("${app.qa.ai-test-generation.enabled:true}")
  private boolean aiTestGenerationEnabled;

  @Value("${app.qa.retrieval.top-k:5}")
  private int topK;

  private final PitchRepository pitchRepository;
  private final MeetingRepository meetingRepository;
  private final ManualNoteRepository manualNoteRepository;
  private final FigmaDesignContextService figmaDesignContextService;
  private final EmbeddingModel embeddingModel;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final ChatLanguageModel chatLanguageModel;
  private final TestCasePromptBuilder promptBuilder;
  private final TestCaseValidator testCaseValidator;

  @Autowired
  public QATestGenerationService(PitchRepository pitchRepository, MeetingRepository meetingRepository,
      ManualNoteRepository manualNoteRepository, FigmaDesignContextService figmaDesignContextService,
      @Autowired(required = false) EmbeddingModel embeddingModel,
      @Autowired(required = false) EmbeddingStore<TextSegment> embeddingStore,
      @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      @Autowired(required = false) TestCasePromptBuilder promptBuilder,
      @Autowired(required = false) TestCaseValidator testCaseValidator) {
    this.pitchRepository = pitchRepository;
    this.meetingRepository = meetingRepository;
    this.manualNoteRepository = manualNoteRepository;
    this.figmaDesignContextService = figmaDesignContextService;
    this.embeddingModel = embeddingModel;
    this.embeddingStore = embeddingStore;
    this.chatLanguageModel = chatLanguageModel;
    this.promptBuilder = promptBuilder;
    this.testCaseValidator = testCaseValidator;
  }

  /** Generate test case suggestions for a pitch using AI. */
  @Transactional(readOnly = true)
  public GenerateTestCasesResponse generateTestCases(GenerateTestCasesRequest request) {
    long startTime = System.currentTimeMillis();

    if (!testManagementEnabled || !aiTestGenerationEnabled) {
      return GenerateTestCasesResponse.builder().aiEnabled(false)
          .errorMessage("AI test generation feature is not enabled")
          .processingTimeMs(System.currentTimeMillis() - startTime).build();
    }

    if (chatLanguageModel == null || embeddingModel == null) {
      return GenerateTestCasesResponse.builder().aiEnabled(false).errorMessage("AI models are not available")
          .processingTimeMs(System.currentTimeMillis() - startTime).build();
    }

    try {
      // Get pitch context
      Pitch pitch = pitchRepository.findById(request.getPitchId())
          .orElseThrow(() -> new RuntimeException("Pitch not found: " + request.getPitchId()));

      // Figma design context is extracted once so the response can report whether it was used —
      // QA needs to know if UI test cases were grounded in the actual design or pitch text only.
      String figmaContext = figmaDesignContextService != null
          ? figmaDesignContextService.extractForPitch(pitch)
          : null;
      boolean figmaContextUsed = figmaContext != null && !figmaContext.isBlank();

      // Build context from pitch description, team notes, meetings, and design context
      String context = buildPitchContext(pitch, request, figmaContext);

      if (context.isEmpty()) {
        return GenerateTestCasesResponse.builder().aiEnabled(true)
            .errorMessage("No context available for this pitch. Add description or meeting notes.")
            .processingTimeMs(System.currentTimeMillis() - startTime).build();
      }

      // Retrieve relevant knowledge if available
      String additionalContext = retrieveRelevantKnowledge(pitch.getTitle() + " " + pitch.getDescription());
      if (!additionalContext.isEmpty()) {
        context += "\n\nRelevant Knowledge:\n" + additionalContext;
      }

      // Retrieve historical test cases for consistency
      List<EmbeddingMatch<TextSegment>> historicalTests = retrieveHistoricalTests(pitch, request);
      log.debug("Retrieved {} historical test examples", historicalTests.size());

      // Determine test type
      TestType testType = determineTestType(request);

      // Generate test cases using type-specific prompt
      String prompt;
      if (promptBuilder != null) {
        prompt = promptBuilder.buildPrompt(pitch, context, request, historicalTests, testType);
      } else {
        prompt = buildTestGenerationPrompt(pitch, context, request);
      }

      String response = chatLanguageModel.generate(prompt);

      // Parse the response into test case suggestions
      List<GenerateTestCasesResponse.TestCaseSuggestion> suggestions = parseTestCaseSuggestions(response);

      // Validate generated test cases
      if (testCaseValidator != null && !suggestions.isEmpty()) {
        TestCaseValidationResult validation = testCaseValidator.validateSuite(suggestions, testType);
        log.debug("Validation result - Valid: {}, Issues: {}", validation.getIsValid(),
            validation.getIssues().size());

        // If validation failed, try to regenerate or return with warnings
        if (!validation.getIsValid()) {
          log.warn("Generated test cases have validation issues: {}", validation.getIssues());
        }
      }

      return GenerateTestCasesResponse.builder().suggestions(suggestions)
          .contextUsed(context.length() > 500 ? context.substring(0, 500) + "..." : context).aiEnabled(true)
          .figmaContextUsed(figmaContextUsed).processingTimeMs(System.currentTimeMillis() - startTime)
          .build();

    } catch (Exception e) {
      log.error("Error generating test cases: {}", e.getMessage(), e);
      return GenerateTestCasesResponse.builder().aiEnabled(true)
          .errorMessage("Failed to generate test cases: " + e.getMessage())
          .processingTimeMs(System.currentTimeMillis() - startTime).build();
    }
  }

  private String buildPitchContext(Pitch pitch, GenerateTestCasesRequest request, String figmaContext) {
    StringBuilder context = new StringBuilder();

    // Add pitch information
    context.append("Pitch: ").append(pitch.getTitle()).append("\n");
    context.append("Status: ").append(pitch.getStatus()).append("\n");
    context.append("Appetite: ").append(pitch.getAppetiteDays()).append(" days\n\n");

    // QA-provided requirements are placed first, with an explicit imperative, so they don't get
    // diluted by the wall of pitch/meeting context that follows — buried mid-context, free-text
    // notes were being effectively ignored by the LLM.
    if (request.getAdditionalContext() != null && !request.getAdditionalContext().isBlank()) {
      context.append("=== QA TEAM REQUIREMENTS (mandatory — every item below MUST be covered by ")
          .append("at least one generated test case) ===\n").append(request.getAdditionalContext())
          .append("\n\n");
    }

    // Add all Shape Up methodology fields for comprehensive test generation
    context.append("=== PITCH DETAILS ===\n");
    if (pitch.getDescription() != null && !pitch.getDescription().isEmpty()) {
      context.append("Description: ").append(pitch.getDescription()).append("\n\n");
    }
    if (pitch.getProblemStatement() != null && !pitch.getProblemStatement().isBlank()) {
      context.append("Problem Statement: ").append(pitch.getProblemStatement()).append("\n\n");
    }
    if (pitch.getSolution() != null && !pitch.getSolution().isBlank()) {
      context.append("Proposed Solution: ").append(pitch.getSolution()).append("\n\n");
    }
    if (pitch.getRabbitHoles() != null && !pitch.getRabbitHoles().isBlank()) {
      context.append("Rabbit Holes (areas to avoid): ").append(pitch.getRabbitHoles()).append("\n\n");
    }
    if (pitch.getRisks() != null && !pitch.getRisks().isBlank()) {
      context.append("Known Risks: ").append(pitch.getRisks()).append("\n\n");
    }
    if (pitch.getNoGos() != null && !pitch.getNoGos().isBlank()) {
      context.append("No-Gos (out of scope): ").append(pitch.getNoGos()).append("\n\n");
    }
    if (pitch.getWireframeLinks() != null && !pitch.getWireframeLinks().isBlank()) {
      context.append("Wireframe/Design Links: ").append(pitch.getWireframeLinks()).append("\n\n");
    }

    if (figmaContext != null && !figmaContext.isBlank()) {
      context.append("=== DESIGN CONTEXT (from Figma) — cover these screens, components and UI "
          + "states in UI-facing test cases ===\n").append(figmaContext).append("\n\n");
    }

    appendManualNotes(context, pitch);

    // Add meeting notes
    List<Meeting> meetings = meetingRepository.findByPitchId(pitch.getId());
    if (!meetings.isEmpty()) {
      context.append("\nMeeting Notes:\n");
      for (Meeting meeting : meetings) {
        if (meeting.getNotes() != null && !meeting.getNotes().isEmpty()) {
          context.append("- [").append(meeting.getType()).append("] ").append(meeting.getNotes())
              .append("\n");
        }
      }
    }

    // Add focus areas
    if (request.getFocusAreas() != null && !request.getFocusAreas().isEmpty()) {
      context.append("\nFocus Areas:\n");
      for (String area : request.getFocusAreas()) {
        context.append("- ").append(area).append("\n");
      }
    }

    return context.toString();
  }

  // Notes can be arbitrarily long (solution designs, data contracts) — cap per-note length and
  // note count so a single verbose note can't crowd the pitch details out of the token budget.
  private static final int MAX_NOTES_IN_CONTEXT = 10;
  private static final int MAX_NOTE_CHARS = 1500;

  /**
   * Append the manual notes team members (QA, developers, designers) attached to the pitch —
   * decisions, data contracts, edge cases captured outside the pitch body. Notes whose author
   * opted out of knowledge sharing ({@code includeInKnowledge=false}) are excluded.
   */
  private void appendManualNotes(StringBuilder context, Pitch pitch) {
    List<ManualNote> notes = manualNoteRepository.findByContextTypeAndContextId("pitch", pitch.getId());
    List<ManualNote> included = notes.stream().filter(n -> !Boolean.FALSE.equals(n.getIncludeInKnowledge()))
        .sorted(Comparator.comparing(ManualNote::getCreatedAt,
            Comparator.nullsFirst(Comparator.naturalOrder())))
        .limit(MAX_NOTES_IN_CONTEXT).collect(Collectors.toList());

    if (included.isEmpty()) {
      return;
    }

    context.append("=== TEAM NOTES (decisions and details added by QA/team members — treat as "
        + "authoritative requirements) ===\n");
    for (ManualNote note : included) {
      String content = note.getContent() == null ? "" : note.getContent();
      if (content.length() > MAX_NOTE_CHARS) {
        content = content.substring(0, MAX_NOTE_CHARS) + " [...truncated]";
      }
      context.append("- ").append(note.getTitle()).append(":\n").append(content).append("\n\n");
    }
  }

  private String retrieveRelevantKnowledge(String query) {
    if (embeddingStore == null || embeddingModel == null) {
      return "";
    }

    try {
      Embedding queryEmbedding = embeddingModel.embed(query).content();
      EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding)
          .maxResults(topK).minScore(0.5).build();

      EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

      return result.matches().stream().map(match -> match.embedded().text()).collect(Collectors.joining("\n\n"));
    } catch (Exception e) {
      log.warn("Could not retrieve relevant knowledge: {}", e.getMessage());
      return "";
    }
  }

  private String buildTestGenerationPrompt(Pitch pitch, String context, GenerateTestCasesRequest request) {
    int maxTestCases = request.getMaxTestCases() != null ? request.getMaxTestCases() : 5;
    String testTypes = request.getTestTypes() != null && !request.getTestTypes().isEmpty()
        ? String.join(", ", request.getTestTypes())
        : "FUNCTIONAL, INTEGRATION, E2E";

    return String.format("""
        You are a QA engineer generating test cases for a software feature.

        Based on the following context, generate %d test cases for the pitch "%s".

        Context:
        %s

        Test Types to focus on: %s

        For each test case, provide:
        1. Title (clear, concise)
        2. Description (what is being tested)
        3. Preconditions (setup required)
        4. Steps (numbered steps to execute)
        5. Expected Result (what should happen)
        6. Suggested Type (FUNCTIONAL, INTEGRATION, UNIT, E2E, REGRESSION, SMOKE)
        7. Suggested Priority (LOW, MEDIUM, HIGH, CRITICAL)
        8. Tags (comma-separated keywords)

        Format each test case as:
        ---TEST CASE---
        TITLE: <title>
        DESCRIPTION: <description>
        PRECONDITIONS: <preconditions>
        STEPS:
        1. <step>
        2. <step>
        ...
        EXPECTED: <expected result>
        TYPE: <type>
        PRIORITY: <priority>
        TAGS: <tag1, tag2, ...>
        ---END---

        Generate comprehensive test cases that cover:
        - Happy path scenarios
        - Edge cases
        - Error handling
        - Integration points
        - User experience aspects
        """, maxTestCases, pitch.getTitle(), context, testTypes);
  }

  private List<GenerateTestCasesResponse.TestCaseSuggestion> parseTestCaseSuggestions(String response) {
    List<GenerateTestCasesResponse.TestCaseSuggestion> suggestions = new ArrayList<>();

    String[] testCases = response.split("---TEST CASE---");
    for (String testCase : testCases) {
      if (!testCase.contains("TITLE:"))
        continue;

      try {
        String title = extractField(testCase, "TITLE:");
        String description = extractField(testCase, "DESCRIPTION:");
        String preconditions = extractField(testCase, "PRECONDITIONS:");
        String steps = extractSteps(testCase);
        String expectedResult = extractField(testCase, "EXPECTED:");
        String type = extractField(testCase, "TYPE:");
        String priority = extractField(testCase, "PRIORITY:");
        String tags = extractField(testCase, "TAGS:");

        if (title != null && !title.isEmpty()) {
          suggestions.add(GenerateTestCasesResponse.TestCaseSuggestion.builder().title(title)
              .description(description).preconditions(preconditions).steps(steps)
              .expectedResult(expectedResult).suggestedType(type).suggestedPriority(priority)
              .suggestedTags(tags != null ? List.of(tags.split(",\\s*")) : null).confidenceScore(0.8)
              .build());
        }
      } catch (Exception e) {
        log.warn("Could not parse test case: {}", e.getMessage());
      }
    }

    return suggestions;
  }

  private static final String[] FIELD_MARKERS = {"TITLE:", "DESCRIPTION:", "PRECONDITIONS:", "STEPS:", "EXPECTED:",
      "TYPE:", "PRIORITY:", "TAGS:", "---END---"};

  // Field values often span multiple lines (e.g. the LLM puts a detailed "Expected Result" on
  // the line(s) after "EXPECTED:" rather than right after the colon). Cutting off at the first
  // "\n" left the value empty in that case — the field must be delimited by the *next field
  // marker*, not the next newline, to capture multi-line content correctly.
  private String extractField(String text, String fieldName) {
    int start = text.indexOf(fieldName);
    if (start < 0)
      return null;

    start += fieldName.length();
    int end = text.length();
    for (String marker : FIELD_MARKERS) {
      if (marker.equals(fieldName))
        continue;
      int markerIdx = text.indexOf(marker, start);
      if (markerIdx >= 0 && markerIdx < end) {
        end = markerIdx;
      }
    }

    String value = text.substring(start, end).trim();
    return value.isEmpty() ? null : value;
  }

  private String extractSteps(String text) {
    return extractField(text, "STEPS:");
  }

  /** Retrieve historical test cases for consistency and learning. */
  private List<EmbeddingMatch<TextSegment>> retrieveHistoricalTests(Pitch pitch, GenerateTestCasesRequest request) {
    if (embeddingStore == null || embeddingModel == null) {
      return new ArrayList<>();
    }

    try {
      // Build query from pitch info
      String query = pitch.getTitle() + " " + (pitch.getDescription() != null ? pitch.getDescription() : "");

      Embedding queryEmbedding = embeddingModel.embed(query).content();
      EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder().queryEmbedding(queryEmbedding)
          .maxResults(5).minScore(0.65) // Only retrieve reasonably similar tests
          .build();

      EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

      // Filter for test case documents
      return result.matches().stream().filter(match -> {
        if (match.embedded().metadata() == null)
          return false;
        String entityType = match.embedded().metadata().getString("entityType");
        return "test_case".equalsIgnoreCase(entityType);
      }).collect(Collectors.toList());

    } catch (Exception e) {
      log.warn("Could not retrieve historical tests: {}", e.getMessage());
      return new ArrayList<>();
    }
  }

  /** Determine the primary test type from request. */
  private TestType determineTestType(GenerateTestCasesRequest request) {
    if (request.getTestTypes() == null || request.getTestTypes().isEmpty()) {
      return TestType.FUNCTIONAL; // Default
    }

    String primaryType = request.getTestTypes().get(0).toUpperCase();
    try {
      return TestType.valueOf(primaryType);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid test type: {}, defaulting to FUNCTIONAL", primaryType);
      return TestType.FUNCTIONAL;
    }
  }
}
