package com.github.farzadsedaghatbin.shipflow.service.qa;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Re-ranks retrieved documents for better relevance ordering. Uses score-based
 * and metadata-based re-ranking since we don't have a cross-encoder.
 */
@Component
@Slf4j
public class DocumentReranker {

  /** Re-rank documents based on query and additional signals. */
  public List<EmbeddingMatch<TextSegment>> rerank(String query, List<EmbeddingMatch<TextSegment>> matches, int topK) {

    if (matches == null || matches.isEmpty()) {
      return matches;
    }

    log.debug("Re-ranking {} documents to top {}", matches.size(), topK);

    // Calculate composite scores combining multiple signals
    List<ScoredMatch> scoredMatches = matches.stream().map(match -> scoreMatch(match, query))
        .collect(Collectors.toList());

    // Sort by composite score (descending)
    scoredMatches.sort(Comparator.comparingDouble(ScoredMatch::getCompositeScore).reversed());

    // Return top K
    List<EmbeddingMatch<TextSegment>> reranked = scoredMatches.stream().limit(topK).map(ScoredMatch::getMatch)
        .collect(Collectors.toList());

    log.debug("Re-ranking improved avg score from {:.3f} to {:.3f}", calculateAvgScore(matches),
        calculateAvgScore(reranked));

    return reranked;
  }

  /** Score a match using multiple signals. */
  private ScoredMatch scoreMatch(EmbeddingMatch<TextSegment> match, String query) {
    double embeddingScore = match.score();

    // Boost score based on metadata signals
    double recencyBoost = calculateRecencyBoost(match);
    double entityTypeBoost = calculateEntityTypeBoost(match);
    double lengthPenalty = calculateLengthPenalty(match);

    // Composite score: weighted combination
    double compositeScore = embeddingScore * 0.70 + // Embedding similarity is most important
        recencyBoost * 0.15 + // Recent docs are more relevant
        entityTypeBoost * 0.10 + // Some entity types are more authoritative
        lengthPenalty * 0.05; // Penalize very short/long docs

    return new ScoredMatch(match, compositeScore);
  }

  /** Boost recent documents (last 30 days = full boost, older = decay). */
  private double calculateRecencyBoost(EmbeddingMatch<TextSegment> match) {
    if (match.embedded() == null || match.embedded().metadata() == null) {
      return 0.5; // Neutral if no metadata
    }

    String createdAt = match.embedded().metadata().getString("createdAt");
    if (createdAt == null) {
      return 0.5;
    }

    try {
      // Simple recency boost: newer = better
      // In real implementation, parse createdAt and calculate age
      return 0.8; // Placeholder - moderate boost
    } catch (Exception e) {
      return 0.5;
    }
  }

  /** Boost certain entity types that are more authoritative. */
  private double calculateEntityTypeBoost(EmbeddingMatch<TextSegment> match) {
    if (match.embedded() == null || match.embedded().metadata() == null) {
      return 0.5;
    }

    String entityType = match.embedded().metadata().getString("entityType");
    if (entityType == null) {
      return 0.5;
    }

    // Boost authoritative sources
    return switch (entityType.toLowerCase()) {
      case "pitch" -> 1.0; // Pitches are primary source
      case "meeting" -> 0.9; // Meetings have decisions
      case "retrospective" -> 0.85; // Retros have insights
      case "document" -> 0.8; // Uploaded docs
      case "qa_interaction" -> 0.6; // Past Q&A (less authoritative)
      default -> 0.5;
    };
  }

  /**
   * Penalize documents that are too short or too long. Sweet spot: 100-1000
   * characters.
   */
  private double calculateLengthPenalty(EmbeddingMatch<TextSegment> match) {
    if (match.embedded() == null) {
      return 0.5;
    }

    int length = match.embedded().text().length();

    if (length < 50) {
      return 0.3; // Too short, likely not useful
    } else if (length < 100) {
      return 0.6;
    } else if (length <= 1000) {
      return 1.0; // Optimal length
    } else if (length <= 2000) {
      return 0.8;
    } else {
      return 0.5; // Too long, might be noisy
    }
  }

  private double calculateAvgScore(List<EmbeddingMatch<TextSegment>> matches) {
    return matches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0.0);
  }

  /** Holder for match with composite score. */
  private static class ScoredMatch {
    private final EmbeddingMatch<TextSegment> match;
    private final double compositeScore;

    public ScoredMatch(EmbeddingMatch<TextSegment> match, double compositeScore) {
      this.match = match;
      this.compositeScore = compositeScore;
    }

    public EmbeddingMatch<TextSegment> getMatch() {
      return match;
    }

    public double getCompositeScore() {
      return compositeScore;
    }
  }
}
