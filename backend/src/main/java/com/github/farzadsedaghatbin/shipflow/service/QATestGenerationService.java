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
  private final UploadedDocumentRepository documentRepository;
  private final TestCaseRepository testCaseRepository;
  private final EmbeddingModel embeddingModel;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final ChatLanguageModel chatLanguageModel;
  private final TestCasePromptBuilder promptBuilder;
  private final TestCaseValidator testCaseValidator;

  @Autowired
  public QATestGenerationService(
      PitchRepository pitchRepository,
      MeetingRepository meetingRepository,
      UploadedDocumentRepository documentRepository,
      TestCaseRepository testCaseRepository,
      @Autowired(required = false) EmbeddingModel embeddingModel,
      @Autowired(required = false) EmbeddingStore<TextSegment> embeddingStore,
      @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      @Autowired(required = false) TestCasePromptBuilder promptBuilder,
      @Autowired(required = false) TestCaseValidator testCaseValidator) {
    this.pitchRepository = pitchRepository;
    this.meetingRepository = meetingRepository;
    this.documentRepository = documentRepository;
    this.testCaseRepository = testCaseRepository;
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
      return GenerateTestCasesResponse.builder()
          .aiEnabled(false)
          .errorMessage("AI test generation feature is not enabled")
          .processingTimeMs(System.currentTimeMillis() - startTime)
          .build();
    }

    if (chatLanguageModel == null || embeddingModel == null) {
      return GenerateTestCasesResponse.builder()
          .aiEnabled(false)
          .errorMessage("AI models are not available")
          .processingTimeMs(System.currentTimeMillis() - startTime)
          .build();
    }

    try {
      // Get pitch context
      Pitch pitch =
          pitchRepository
              .findById(request.getPitchId())
              .orElseThrow(() -> new RuntimeException("Pitch not found: " + request.getPitchId()));

      // Build context from pitch description, meetings, and documents
      String context = buildPitchContext(pitch, request);

      if (context.isEmpty()) {
        return GenerateTestCasesResponse.builder()
            .aiEnabled(true)
            .errorMessage("No context available for this pitch. Add description or meeting notes.")
            .processingTimeMs(System.currentTimeMillis() - startTime)
            .build();
      }

      // Retrieve relevant knowledge if available
      String additionalContext =
          retrieveRelevantKnowledge(pitch.getTitle() + " " + pitch.getDescription());
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
      List<GenerateTestCasesResponse.TestCaseSuggestion> suggestions =
          parseTestCaseSuggestions(response);

      // Validate generated test cases
      if (testCaseValidator != null && !suggestions.isEmpty()) {
        TestCaseValidationResult validation =
            testCaseValidator.validateSuite(suggestions, testType);
        log.debug(
            "Validation result - Valid: {}, Issues: {}",
            validation.getIsValid(),
            validation.getIssues().size());

        // If validation failed, try to regenerate or return with warnings
        if (!validation.getIsValid()) {
          log.warn("Generated test cases have validation issues: {}", validation.getIssues());
        }
      }

      return GenerateTestCasesResponse.builder()
          .suggestions(suggestions)
          .contextUsed(context.length() > 500 ? context.substring(0, 500) + "..." : context)
          .aiEnabled(true)
          .processingTimeMs(System.currentTimeMillis() - startTime)
          .build();

    } catch (Exception e) {
      log.error("Error generating test cases: {}", e.getMessage(), e);
      return GenerateTestCasesResponse.builder()
          .aiEnabled(true)
          .errorMessage("Failed to generate test cases: " + e.getMessage())
          .processingTimeMs(System.currentTimeMillis() - startTime)
          .build();
    }
  }

  private String buildPitchContext(Pitch pitch, GenerateTestCasesRequest request) {
    StringBuilder context = new StringBuilder();

    // Add pitch information
    context.append("Pitch: ").append(pitch.getTitle()).append("\n");
    if (pitch.getDescription() != null && !pitch.getDescription().isEmpty()) {
      context.append("Description: ").append(pitch.getDescription()).append("\n");
    }
    context.append("Status: ").append(pitch.getStatus()).append("\n");
    context.append("Appetite: ").append(pitch.getAppetiteDays()).append(" days\n");

    // Add meeting notes
    List<Meeting> meetings = meetingRepository.findByPitchId(pitch.getId());
    if (!meetings.isEmpty()) {
      context.append("\nMeeting Notes:\n");
      for (Meeting meeting : meetings) {
        if (meeting.getNotes() != null && !meeting.getNotes().isEmpty()) {
          context
              .append("- [")
              .append(meeting.getType())
              .append("] ")
              .append(meeting.getNotes())
              .append("\n");
        }
      }
    }

    // Add additional context from request
    if (request.getAdditionalContext() != null && !request.getAdditionalContext().isEmpty()) {
      context.append("\nAdditional Context:\n").append(request.getAdditionalContext()).append("\n");
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

  private String retrieveRelevantKnowledge(String query) {
    if (embeddingStore == null || embeddingModel == null) {
      return "";
    }

    try {
      Embedding queryEmbedding = embeddingModel.embed(query).content();
      EmbeddingSearchRequest searchRequest =
          EmbeddingSearchRequest.builder()
              .queryEmbedding(queryEmbedding)
              .maxResults(topK)
              .minScore(0.5)
              .build();

      EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

      return result.matches().stream()
          .map(match -> match.embedded().text())
          .collect(Collectors.joining("\n\n"));
    } catch (Exception e) {
      log.warn("Could not retrieve relevant knowledge: {}", e.getMessage());
      return "";
    }
  }

  private String buildTestGenerationPrompt(
      Pitch pitch, String context, GenerateTestCasesRequest request) {
    int maxTestCases = request.getMaxTestCases() != null ? request.getMaxTestCases() : 5;
    String testTypes =
        request.getTestTypes() != null && !request.getTestTypes().isEmpty()
            ? String.join(", ", request.getTestTypes())
            : "FUNCTIONAL, INTEGRATION, E2E";

    return String.format(
        """
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
            """,
        maxTestCases, pitch.getTitle(), context, testTypes);
  }

  private List<GenerateTestCasesResponse.TestCaseSuggestion> parseTestCaseSuggestions(
      String response) {
    List<GenerateTestCasesResponse.TestCaseSuggestion> suggestions = new ArrayList<>();

    String[] testCases = response.split("---TEST CASE---");
    for (String testCase : testCases) {
      if (!testCase.contains("TITLE:")) continue;

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
          suggestions.add(
              GenerateTestCasesResponse.TestCaseSuggestion.builder()
                  .title(title)
                  .description(description)
                  .preconditions(preconditions)
                  .steps(steps)
                  .expectedResult(expectedResult)
                  .suggestedType(type)
                  .suggestedPriority(priority)
                  .suggestedTags(tags != null ? List.of(tags.split(",\\s*")) : null)
                  .confidenceScore(0.8)
                  .build());
        }
      } catch (Exception e) {
        log.warn("Could not parse test case: {}", e.getMessage());
      }
    }

    return suggestions;
  }

  private String extractField(String text, String fieldName) {
    int start = text.indexOf(fieldName);
    if (start < 0) return null;

    start += fieldName.length();
    int end = text.indexOf("\n", start);
    if (end < 0) end = text.length();

    String value = text.substring(start, end).trim();

    // Handle multi-line fields that end at next field
    String[] nextFields = {
      "TITLE:",
      "DESCRIPTION:",
      "PRECONDITIONS:",
      "STEPS:",
      "EXPECTED:",
      "TYPE:",
      "PRIORITY:",
      "TAGS:",
      "---END---"
    };
    for (String field : nextFields) {
      if (!field.equals(fieldName)) {
        int fieldIdx = value.indexOf(field);
        if (fieldIdx > 0) {
          value = value.substring(0, fieldIdx).trim();
        }
      }
    }

    return value.isEmpty() ? null : value;
  }

  private String extractSteps(String text) {
    int start = text.indexOf("STEPS:");
    if (start < 0) return null;

    start += "STEPS:".length();
    int end = text.indexOf("EXPECTED:");
    if (end < 0) end = text.length();

    return text.substring(start, end).trim();
  }

  /** Retrieve historical test cases for consistency and learning. */
  private List<EmbeddingMatch<TextSegment>> retrieveHistoricalTests(
      Pitch pitch, GenerateTestCasesRequest request) {
    if (embeddingStore == null || embeddingModel == null) {
      return new ArrayList<>();
    }

    try {
      // Build query from pitch info
      String query =
          pitch.getTitle() + " " + (pitch.getDescription() != null ? pitch.getDescription() : "");

      Embedding queryEmbedding = embeddingModel.embed(query).content();
      EmbeddingSearchRequest searchRequest =
          EmbeddingSearchRequest.builder()
              .queryEmbedding(queryEmbedding)
              .maxResults(5)
              .minScore(0.65) // Only retrieve reasonably similar tests
              .build();

      EmbeddingSearchResult<TextSegment> result = embeddingStore.search(searchRequest);

      // Filter for test case documents
      return result.matches().stream()
          .filter(
              match -> {
                if (match.embedded().metadata() == null) return false;
                String entityType = match.embedded().metadata().getString("entityType");
                return "test_case".equalsIgnoreCase(entityType);
              })
          .collect(Collectors.toList());

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
