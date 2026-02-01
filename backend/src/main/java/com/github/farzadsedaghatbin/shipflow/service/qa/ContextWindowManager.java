package com.github.farzadsedaghatbin.shipflow.service.qa;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Manages context window to prevent token limit exceeded errors. Estimates tokens and truncates
 * context intelligently.
 */
@Component
@Slf4j
public class ContextWindowManager {

  // Conservative token limits for different models
  private static final int DEFAULT_MAX_TOKENS = 4000; // Claude Sonnet default
  private static final int PROMPT_OVERHEAD_TOKENS = 500; // System prompt + question
  private static final double CHARS_PER_TOKEN = 4.0; // Rough estimate: 1 token ≈ 4 chars

  /** Build context from matches while respecting token limits. */
  public ContextResult buildManagedContext(
      List<EmbeddingMatch<TextSegment>> matches, String question, int maxTokens) {

    if (matches == null || matches.isEmpty()) {
      return new ContextResult("", 0, 0, false);
    }

    // Calculate available tokens for context
    int questionTokens = estimateTokens(question);
    int availableTokens =
        (maxTokens > 0 ? maxTokens : DEFAULT_MAX_TOKENS) - PROMPT_OVERHEAD_TOKENS - questionTokens;

    log.debug(
        "Building context with {} available tokens (max: {}, question: {}, overhead: {})",
        availableTokens,
        maxTokens,
        questionTokens,
        PROMPT_OVERHEAD_TOKENS);

    StringBuilder context = new StringBuilder();
    int usedTokens = 0;
    int documentsIncluded = 0;
    boolean wasTruncated = false;

    // Add documents until we hit token limit
    for (int i = 0; i < matches.size(); i++) {
      EmbeddingMatch<TextSegment> match = matches.get(i);
      String docText = match.embedded().text();
      String title = extractTitle(match);

      // Format document
      String formattedDoc = String.format("[Source %d: %s]\n%s\n\n", i + 1, title, docText);

      int docTokens = estimateTokens(formattedDoc);

      // Check if adding this doc would exceed limit
      if (usedTokens + docTokens > availableTokens) {
        // Try to add a truncated version
        int remainingTokens = availableTokens - usedTokens;
        if (remainingTokens > 100) { // Only truncate if we have meaningful space
          String truncated =
              truncateToTokens(docText, remainingTokens - 50); // Reserve tokens for formatting
          formattedDoc =
              String.format("[Source %d: %s] (truncated)\n%s...\n\n", i + 1, title, truncated);
          context.append(formattedDoc);
          usedTokens += estimateTokens(formattedDoc);
          documentsIncluded++;
          wasTruncated = true;
        }
        // Stop adding documents
        log.debug("Reached token limit at document {}/{}", i + 1, matches.size());
        wasTruncated = true;
        break;
      }

      context.append(formattedDoc);
      usedTokens += docTokens;
      documentsIncluded++;
    }

    if (wasTruncated) {
      log.warn(
          "Context truncated: included {}/{} documents, {} tokens used of {} available",
          documentsIncluded,
          matches.size(),
          usedTokens,
          availableTokens);
    } else {
      log.debug(
          "Context built: {}/{} documents, {} tokens used of {} available",
          documentsIncluded,
          matches.size(),
          usedTokens,
          availableTokens);
    }

    return new ContextResult(context.toString(), usedTokens, documentsIncluded, wasTruncated);
  }

  /**
   * Estimate token count from text. This is a rough approximation. For production, use proper
   * tokenizer.
   */
  public int estimateTokens(String text) {
    if (text == null || text.isEmpty()) {
      return 0;
    }
    return (int) Math.ceil(text.length() / CHARS_PER_TOKEN);
  }

  /** Truncate text to approximately fit in token count. */
  private String truncateToTokens(String text, int maxTokens) {
    if (text == null || text.isEmpty()) {
      return "";
    }

    int maxChars = (int) (maxTokens * CHARS_PER_TOKEN);
    if (text.length() <= maxChars) {
      return text;
    }

    // Truncate at sentence boundary if possible
    String truncated = text.substring(0, maxChars);
    int lastPeriod = truncated.lastIndexOf('.');
    int lastNewline = truncated.lastIndexOf('\n');

    int cutoff = Math.max(lastPeriod, lastNewline);
    if (cutoff > maxChars / 2) { // Only use boundary if it's not too early
      return text.substring(0, cutoff + 1);
    }

    return truncated;
  }

  private String extractTitle(EmbeddingMatch<TextSegment> match) {
    if (match.embedded().metadata() != null) {
      String title = match.embedded().metadata().getString("title");
      if (title != null && !title.isEmpty()) {
        return title;
      }
    }
    return "Unknown";
  }

  /** Result of context building with metadata. */
  public static class ContextResult {
    private final String context;
    private final int tokensUsed;
    private final int documentsIncluded;
    private final boolean wasTruncated;

    public ContextResult(
        String context, int tokensUsed, int documentsIncluded, boolean wasTruncated) {
      this.context = context;
      this.tokensUsed = tokensUsed;
      this.documentsIncluded = documentsIncluded;
      this.wasTruncated = wasTruncated;
    }

    public String getContext() {
      return context;
    }

    public int getTokensUsed() {
      return tokensUsed;
    }

    public int getDocumentsIncluded() {
      return documentsIncluded;
    }

    public boolean isWasTruncated() {
      return wasTruncated;
    }
  }
}
