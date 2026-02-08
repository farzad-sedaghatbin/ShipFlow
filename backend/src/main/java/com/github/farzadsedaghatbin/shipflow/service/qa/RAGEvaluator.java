package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.dto.qa.RAGEvaluationMetrics;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Evaluator for RAG (Retrieval-Augmented Generation) quality metrics. */
@Component
@Slf4j
public class RAGEvaluator {

  private static final Pattern SOURCE_CITATION_PATTERN = Pattern.compile("\\[Source \\d+");

  /** Evaluate RAG quality for a Q&A interaction. */
  public RAGEvaluationMetrics evaluate(String question, String answer,
      List<EmbeddingMatch<TextSegment>> retrievedDocs) {

    // Calculate retrieval relevance
    double retrievalRelevance = calculateRetrievalRelevance(retrievedDocs);

    // Calculate faithfulness (grounding in sources)
    double faithfulness = calculateFaithfulness(answer, retrievedDocs);

    // Calculate answer relevance to question
    double answerRelevance = calculateAnswerRelevance(question, answer);

    // Calculate context utilization
    double contextUtilization = calculateContextUtilization(answer, retrievedDocs);

    // Count documents
    int docsRetrieved = retrievedDocs.size();
    int docsCited = countCitations(answer);

    // Average similarity score
    double avgSimilarity = retrievedDocs.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0.0);

    return RAGEvaluationMetrics.builder().retrievalRelevance(retrievalRelevance).faithfulness(faithfulness)
        .answerRelevance(answerRelevance).contextUtilization(contextUtilization)
        .documentsRetrieved(docsRetrieved).documentsCited(docsCited).averageSimilarityScore(avgSimilarity * 100)
        .build();
  }

  /**
   * Calculate how relevant the retrieved documents are. Based on similarity
   * scores and document count.
   */
  private double calculateRetrievalRelevance(List<EmbeddingMatch<TextSegment>> retrievedDocs) {
    if (retrievedDocs.isEmpty())
      return 0.0;

    // Average similarity score
    double avgScore = retrievedDocs.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0.0);

    // Bonus for having multiple relevant sources (up to 5)
    double diversityBonus = Math.min(retrievedDocs.size() / 5.0, 1.0) * 10;

    return Math.min(100.0, (avgScore * 90) + diversityBonus);
  }

  /**
   * Calculate faithfulness - whether answer stays grounded in sources. Low score
   * indicates potential hallucination.
   */
  private double calculateFaithfulness(String answer, List<EmbeddingMatch<TextSegment>> retrievedDocs) {
    if (answer == null || answer.isEmpty())
      return 0.0;
    if (retrievedDocs.isEmpty())
      return 50.0; // Neutral if no sources

    // Check for explicit statements that indicate no information found
    if (answer.toLowerCase().contains("couldn't find") || answer.toLowerCase().contains("no relevant information")
        || answer.toLowerCase().contains("don't have enough information")) {
      return 100.0; // Honest about limitations = high faithfulness
    }

    // Count how many source texts are referenced in the answer
    int sourcesUsed = 0;
    for (EmbeddingMatch<TextSegment> match : retrievedDocs) {
      String sourceText = match.embedded().text().toLowerCase();
      String answerLower = answer.toLowerCase();

      // Check for direct quotes or paraphrases
      String[] sourceWords = sourceText.split("\\s+");
      int matchingWords = 0;
      for (String word : sourceWords) {
        if (word.length() > 4 && answerLower.contains(word)) {
          matchingWords++;
        }
      }

      // If significant portion of source is used, count it
      if (matchingWords > Math.min(sourceWords.length * 0.1, 5)) {
        sourcesUsed++;
      }
    }

    // Score based on source usage
    double usageRatio = (double) sourcesUsed / retrievedDocs.size();
    return usageRatio * 100;
  }

  /** Calculate how well the answer addresses the question. */
  private double calculateAnswerRelevance(String question, String answer) {
    if (question == null || answer == null)
      return 0.0;

    String questionLower = question.toLowerCase();
    String answerLower = answer.toLowerCase();

    // Extract key terms from question (remove common words)
    String[] questionWords = questionLower.split("\\s+");
    int keyTermsFound = 0;
    int totalKeyTerms = 0;

    for (String word : questionWords) {
      // Skip common words
      if (word.length() <= 3 || isCommonWord(word))
        continue;

      totalKeyTerms++;
      if (answerLower.contains(word)) {
        keyTermsFound++;
      }
    }

    if (totalKeyTerms == 0)
      return 70.0; // Default for short questions

    double termCoverage = (double) keyTermsFound / totalKeyTerms;

    // Bonus for not being too short or too generic
    int answerLength = answer.split("\\s+").length;
    double lengthScore = answerLength > 10 && answerLength < 500 ? 1.0 : 0.8;

    return Math.min(100.0, termCoverage * 90 * lengthScore);
  }

  /** Calculate what percentage of retrieved context was actually used. */
  private double calculateContextUtilization(String answer, List<EmbeddingMatch<TextSegment>> retrievedDocs) {
    if (retrievedDocs.isEmpty())
      return 0.0;

    int totalSources = retrievedDocs.size();
    int citedSources = countCitations(answer);

    return Math.min(100.0, ((double) citedSources / totalSources) * 100);
  }

  /** Count number of source citations in the answer. */
  private int countCitations(String answer) {
    if (answer == null)
      return 0;

    Matcher matcher = SOURCE_CITATION_PATTERN.matcher(answer);
    int count = 0;
    while (matcher.find()) {
      count++;
    }
    return count;
  }

  /** Check if a word is too common to be meaningful. */
  private boolean isCommonWord(String word) {
    Set<String> commonWords = Set.of("the", "is", "are", "was", "were", "has", "have", "had", "what", "when",
        "where", "who", "why", "how", "which", "this", "that", "these", "those", "and", "or", "but", "for",
        "with", "about", "from", "into", "during", "before", "after");
    return commonWords.contains(word);
  }
}
