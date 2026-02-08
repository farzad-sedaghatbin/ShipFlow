package com.github.farzadsedaghatbin.shipflow.service.qa;

import dev.langchain4j.model.chat.ChatLanguageModel;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Decomposes complex questions into simpler sub-queries for better RAG
 * retrieval.
 *
 * <p>
 * Examples: - "Compare payment feature risks vs checkout redesign" → ["What are
 * the risks for payment feature?", "What are the risks for checkout redesign?"]
 * - "Which team is faster on backend work?" → ["What teams work on backend?",
 * "What is the velocity of each team?"]
 */
@Slf4j
@Component
public class QueryDecomposer {

  private final ChatLanguageModel chatLanguageModel;

  public QueryDecomposer(@Autowired(required = false) ChatLanguageModel chatLanguageModel) {
    this.chatLanguageModel = chatLanguageModel;
  }

  /** Determines if a query is complex enough to benefit from decomposition. */
  public boolean shouldDecompose(String query) {
    if (query == null || query.length() < 20) {
      return false;
    }

    // Indicators of complex queries
    String lowerQuery = query.toLowerCase();
    return lowerQuery.contains("compare") || lowerQuery.contains("versus") || lowerQuery.contains(" vs ")
        || lowerQuery.contains(" and ") && (lowerQuery.contains("which") || lowerQuery.contains("what"))
        || lowerQuery.contains("both") || lowerQuery.contains("all") && lowerQuery.contains("?")
        || (lowerQuery.split("\\?").length > 1); // Multiple questions
  }

  /** Decomposes a complex query into simpler sub-queries. */
  public List<String> decompose(String query) {
    if (!shouldDecompose(query) || chatLanguageModel == null) {
      return List.of(query);
    }

    try {
      String prompt = buildDecompositionPrompt(query);
      String response = chatLanguageModel.generate(prompt);

      List<String> subQueries = parseSubQueries(response);

      if (subQueries.isEmpty()) {
        log.warn("Decomposition produced no sub-queries, using original: {}", query);
        return List.of(query);
      }

      log.debug("Decomposed query into {} sub-queries: {}", subQueries.size(), subQueries);
      return subQueries;

    } catch (Exception e) {
      log.error("Failed to decompose query, using original: {}", e.getMessage());
      return List.of(query);
    }
  }

  /** Builds the decomposition prompt for the LLM. */
  private String buildDecompositionPrompt(String query) {
    return String.format("""
        You are a query analysis expert. Break down the following complex question into simpler sub-questions.
        Each sub-question should be answerable independently and focus on a single concept.

        Rules:
        1. Number each sub-question (1., 2., 3., etc.)
        2. Keep sub-questions simple and focused
        3. Preserve context from the original question
        4. If the question is already simple, return just "1. [original question]"

        Examples:

        Question: "Compare payment feature risks vs checkout redesign risks"
        Sub-questions:
        1. What are the risks for the payment feature?
        2. What are the risks for the checkout redesign?

        Question: "Which team is faster on backend work?"
        Sub-questions:
        1. What teams work on backend development?
        2. What is the velocity of each backend team?

        Question: "What is the status of the pitch?"
        Sub-questions:
        1. What is the status of the pitch?

        Now break down this question:

        Question: "%s"
        Sub-questions:
        """, query);
  }

  /** Parses numbered sub-queries from LLM response. */
  private List<String> parseSubQueries(String response) {
    List<String> subQueries = new ArrayList<>();

    // Pattern to match numbered queries: "1. Question text" or "1) Question text"
    Pattern pattern = Pattern.compile("^\\s*\\d+[.)]\\s*(.+)$", Pattern.MULTILINE);
    Matcher matcher = pattern.matcher(response);

    while (matcher.find()) {
      String subQuery = matcher.group(1).trim();
      if (!subQuery.isEmpty()) {
        subQueries.add(subQuery);
      }
    }

    return subQueries;
  }

  /** Result object containing sub-queries and synthesis instruction. */
  public static class DecompositionResult {
    private final List<String> subQueries;
    private final boolean wasDecomposed;

    public DecompositionResult(List<String> subQueries, boolean wasDecomposed) {
      this.subQueries = subQueries;
      this.wasDecomposed = wasDecomposed;
    }

    public List<String> getSubQueries() {
      return subQueries;
    }

    public boolean wasDecomposed() {
      return wasDecomposed;
    }

    public int getSubQueryCount() {
      return subQueries.size();
    }
  }

  /** Full decomposition with metadata. */
  public DecompositionResult decomposeWithMetadata(String query) {
    boolean shouldDecomp = shouldDecompose(query);
    List<String> subQueries = shouldDecomp ? decompose(query) : List.of(query);
    return new DecompositionResult(subQueries, shouldDecomp && subQueries.size() > 1);
  }
}
