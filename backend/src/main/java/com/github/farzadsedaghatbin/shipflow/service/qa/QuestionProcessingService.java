package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.dto.qa.AskQuestionRequest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for processing and preparing questions before retrieval.
 * Handles query decomposition, context extraction, and ambiguity detection.
 */
@Service
@Slf4j
public class QuestionProcessingService {

  private final QueryDecomposer queryDecomposer;

  @Autowired
  public QuestionProcessingService(@Autowired(required = false) QueryDecomposer queryDecomposer) {
    this.queryDecomposer = queryDecomposer;
  }

  /**
   * Process the question for search preparation.
   * Returns a processing result with decomposed queries if applicable.
   */
  public QuestionProcessingResult process(AskQuestionRequest request) {
    String originalQuestion = request.getQuestion();
    List<String> subQueries = List.of(originalQuestion);
    boolean wasDecomposed = false;

    // Attempt query decomposition for complex questions
    if (queryDecomposer != null && queryDecomposer.shouldDecompose(originalQuestion)) {
      try {
        QueryDecomposer.DecompositionResult decomposition = 
            queryDecomposer.decomposeWithMetadata(originalQuestion);
        if (decomposition.wasDecomposed()) {
          subQueries = decomposition.getSubQueries();
          wasDecomposed = true;
          log.info("Decomposed query into {} sub-queries", subQueries.size());
        }
      } catch (Exception e) {
        log.warn("Query decomposition failed, using original query: {}", e.getMessage());
      }
    }

    // Expand vague questions if needed
    String expandedQuestion = expandVagueQuestion(
        originalQuestion, 
        request.getContextType(), 
        request.getContextId()
    );

    return QuestionProcessingResult.builder()
        .originalQuestion(originalQuestion)
        .expandedQuestion(expandedQuestion)
        .subQueries(subQueries)
        .wasDecomposed(wasDecomposed)
        .searchTerms(extractSearchTerms(originalQuestion))
        .build();
  }

  /**
   * Check if the question contains ambiguous contextual references.
   * Returns a clarification message if ambiguous, null otherwise.
   */
  public String checkForAmbiguousContext(AskQuestionRequest request) {
    String question = request.getQuestion().toLowerCase();

    // Only check for ambiguity if contextType is set but contextId is null
    if (request.getContextType() == null || request.getContextId() != null) {
      return null;
    }

    // Check for "this cycle" or "the cycle" - truly ambiguous references
    if (request.getContextType().equalsIgnoreCase("cycle")) {
      if (question.contains("this cycle") || question.contains("the cycle")) {
        return "I noticed you're asking about a cycle, but I need to know which cycle you're referring to. "
            + "Could you please specify which cycle you'd like to know about? For example: "
            + "\"What pitches are in cycle 5?\" or visit a specific cycle page to ask contextual questions.";
      }
    }

    // Check for "this pitch" or "the pitch"
    if (request.getContextType().equalsIgnoreCase("pitch")) {
      if (question.contains("this pitch") || question.contains("the pitch")) {
        return "I noticed you're asking about a pitch, but I need to know which pitch you're referring to. "
            + "Could you please specify the pitch name or visit a specific pitch page to ask contextual questions.";
      }
    }

    // Check for "this team" or "the team"
    if (request.getContextType().equalsIgnoreCase("team")) {
      if (question.contains("this team") || question.contains("the team")) {
        return "I noticed you're asking about a team, but I need to know which team you're referring to. "
            + "Could you please specify the team name or visit a specific team page to ask contextual questions.";
      }
    }

    // Check for "this meeting" or "the meeting"
    if (request.getContextType().equalsIgnoreCase("meeting")) {
      if (question.contains("this meeting") || question.contains("the meeting")) {
        return "I noticed you're asking about a meeting, but I need to know which meeting you're referring to. "
            + "Could you please specify the meeting or visit a specific meeting page to ask contextual questions?";
      }
    }

    return null;
  }

  /**
   * Generate clarification suggestions when ambiguous context is detected.
   */
  public List<String> generateClarificationSuggestions(AskQuestionRequest request) {
    List<String> suggestions = new ArrayList<>();

    if (request.getContextType() != null) {
      switch (request.getContextType().toLowerCase()) {
        case "cycle":
          suggestions.add("What pitches are in cycle 5?");
          suggestions.add("Show me the latest cycle");
          suggestions.add("List all active cycles");
          break;
        case "pitch":
          suggestions.add("Tell me about the Email Notification System pitch");
          suggestions.add("What pitches need attention?");
          suggestions.add("List all pitches");
          break;
        case "team":
          suggestions.add("What is the Frontend Team working on?");
          suggestions.add("Show me all teams");
          break;
        case "meeting":
          suggestions.add("What was discussed in the latest kickoff meeting?");
          suggestions.add("Show recent meetings");
          break;
        default:
          suggestions.add("Tell me about the current cycle");
          suggestions.add("What pitches need attention?");
          break;
      }
    } else {
      suggestions.add("Tell me about the current cycle");
      suggestions.add("What pitches need attention?");
      suggestions.add("Show me recent team activities");
    }

    return suggestions.subList(0, Math.min(3, suggestions.size()));
  }

  /**
   * Expand vague questions into more searchable queries.
   */
  public String expandVagueQuestion(String question, String contextType, Long contextId) {
    if (question == null || question.trim().isEmpty()) {
      return question;
    }

    String trimmed = question.trim();
    String lowerQuestion = trimmed.toLowerCase();

    // Check if question is very short (entity name/number or 1-2 words)
    String[] words = trimmed.split("\\s+");
    boolean isVague = words.length <= 2 || lowerQuestion
        .matches("^(\\d+|" + (contextType != null ? contextType.toLowerCase() + "\\s*\\d+" : "\\d+") + ")$");

    if (!isVague) {
      return question;
    }

    // Expand based on context type
    if (contextType != null && contextId != null) {
      switch (contextType.toLowerCase()) {
        case "cycle":
          return "What pitches are in cycle " + contextId + "?";
        case "pitch":
          return "Tell me about pitch " + contextId;
        case "team":
          return "What is team " + contextId + " working on?";
        case "meeting":
          return "What was discussed in meeting " + contextId + "?";
        default:
          return "Tell me about " + contextType + " " + contextId;
      }
    }

    return question;
  }

  /**
   * Extract entity ID from question text based on context type.
   */
  public Long extractEntityIdFromQuestion(String question, String contextType) {
    if (question == null || contextType == null) {
      return null;
    }

    String lowerQuestion = question.toLowerCase().trim();
    String lowerContextType = contextType.toLowerCase();

    // Pattern 1: "cycle 4", "pitch 15"
    Pattern pattern1 = Pattern.compile("\\b" + lowerContextType + "\\s+(\\d+)\\b");
    Matcher matcher1 = pattern1.matcher(lowerQuestion);
    if (matcher1.find()) {
      try {
        return Long.parseLong(matcher1.group(1));
      } catch (NumberFormatException e) {
        log.warn("Failed to parse ID from: {}", matcher1.group(1));
      }
    }

    // Pattern 2: Just a number
    if (lowerQuestion.matches("^\\d+$")) {
      try {
        return Long.parseLong(lowerQuestion);
      } catch (NumberFormatException e) {
        log.warn("Failed to parse ID from: {}", lowerQuestion);
      }
    }

    // Pattern 3: "in cycle 4", "for pitch 15"
    Pattern pattern3 = Pattern.compile("\\b(?:in|for|about|from)\\s+" + lowerContextType + "\\s+(\\d+)\\b");
    Matcher matcher3 = pattern3.matcher(lowerQuestion);
    if (matcher3.find()) {
      try {
        return Long.parseLong(matcher3.group(1));
      } catch (NumberFormatException e) {
        log.warn("Failed to parse ID from: {}", matcher3.group(1));
      }
    }

    return null;
  }

  /**
   * Check if a question is likely asking for an entity by ID only.
   */
  public boolean isLikelyIdOnlyQuery(String question) {
    if (question == null) {
      return false;
    }

    String trimmed = question.trim();
    String lowerQuestion = trimmed.toLowerCase();

    // Just a number: "4"
    if (lowerQuestion.matches("^\\d+$")) {
      return true;
    }

    // Entity type + number: "cycle 4"
    if (lowerQuestion.matches("^(cycle|pitch|team|meeting)\\s+\\d+$")) {
      return true;
    }

    // Has additional text that looks like a name/title
    if (lowerQuestion.matches(".*(planning|build|q\\d|phase|sprint|\\w{5,}).*")) {
      return false;
    }

    // Short queries (1-2 words) are likely ID queries
    return trimmed.split("\\s+").length <= 2;
  }

  /**
   * Extract meaningful search terms from a question.
   */
  public List<String> extractSearchTerms(String question) {
    if (question == null || question.trim().isEmpty()) {
      return Collections.emptyList();
    }

    List<String> terms = new ArrayList<>();
    String lowerQuestion = question.toLowerCase().trim();

    // Extract entity + number patterns
    Pattern entityPattern = Pattern.compile("(cycle|pitch|team|meeting)\\s+\\d+");
    Matcher entityMatcher = entityPattern.matcher(lowerQuestion);
    while (entityMatcher.find()) {
      terms.add(entityMatcher.group().trim());
    }

    // Extract and convert status-related terms
    if (lowerQuestion.contains("at-risk") || lowerQuestion.contains("at risk") || lowerQuestion.contains("risk")) {
      terms.add("at-risk");
      terms.add("at_risk");
      terms.add("risk");
    }

    if (lowerQuestion.contains("in progress") || lowerQuestion.contains("in-progress")
        || lowerQuestion.contains("progress")) {
      terms.add("in_progress");
      terms.add("in progress");
      terms.add("progress");
    }

    if (lowerQuestion.contains("started") || lowerQuestion.contains("start")) {
      terms.add("started");
      terms.add("start");
    }

    if (lowerQuestion.contains("completed") || lowerQuestion.contains("complete")
        || lowerQuestion.contains("done")) {
      terms.add("completed");
      terms.add("complete");
      terms.add("done");
    }

    // Extract hyphenated words
    Pattern hyphenPattern = Pattern.compile("\\b\\w++-\\w++\\b");
    Matcher hyphenMatcher = hyphenPattern.matcher(lowerQuestion);
    while (hyphenMatcher.find()) {
      String hyphenated = hyphenMatcher.group().trim();
      terms.add(hyphenated);
      terms.add(hyphenated.replace("-", "_"));
    }

    // Remove common question words
    String cleaned = lowerQuestion.replaceAll(
        "\\b(what|where|when|who|why|how|is|are|the|a|an|in|on|at|for|to|of|about|tell|me|show|any|there)\\b",
        " ").replaceAll("\\s+", " ").trim();

    // Extract significant words
    String[] words = cleaned.split("\\s+");
    for (String word : words) {
      if (word.length() >= 3 && !word.matches("\\d+")) {
        terms.add(word);
      }
    }

    // Add standalone numbers
    Pattern numberPattern = Pattern.compile("\\b\\d+\\b");
    Matcher numberMatcher = numberPattern.matcher(lowerQuestion);
    while (numberMatcher.find()) {
      terms.add(numberMatcher.group());
    }

    return terms.stream()
        .filter(t -> t != null && !t.trim().isEmpty())
        .distinct()
        .collect(Collectors.toList());
  }

  /**
   * Result of question processing.
   */
  @Getter
  @Builder
  @AllArgsConstructor
  public static class QuestionProcessingResult {
    private final String originalQuestion;
    private final String expandedQuestion;
    private final List<String> subQueries;
    private final boolean wasDecomposed;
    private final List<String> searchTerms;
  }
}
