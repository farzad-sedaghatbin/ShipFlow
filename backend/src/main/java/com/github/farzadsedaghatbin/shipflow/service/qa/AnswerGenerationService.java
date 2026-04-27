package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.dto.qa.AskQuestionRequest;
import com.github.farzadsedaghatbin.shipflow.dto.qa.QAResponse;
import com.github.farzadsedaghatbin.shipflow.dto.qa.RAGEvaluationMetrics;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Service for LLM-based answer generation.
 * Handles prompt construction, rate limit handling, and confidence scoring.
 */
@Service
@Slf4j
public class AnswerGenerationService {

  private final ChatLanguageModel chatLanguageModel;
  private final LLMCacheService llmCacheService;
  private final PromptCompressor promptCompressor;
  private final ContentGuardrails contentGuardrails;
  private final FeedbackLearningService feedbackLearningService;
  private final RAGEvaluator ragEvaluator;
  private final ConversationManager conversationManager;

  @Autowired
  public AnswerGenerationService(
      @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      @Autowired(required = false) LLMCacheService llmCacheService,
      @Autowired(required = false) PromptCompressor promptCompressor,
      @Autowired(required = false) ContentGuardrails contentGuardrails,
      @Autowired(required = false) FeedbackLearningService feedbackLearningService,
      @Autowired(required = false) RAGEvaluator ragEvaluator,
      @Autowired(required = false) ConversationManager conversationManager) {
    this.chatLanguageModel = chatLanguageModel;
    this.llmCacheService = llmCacheService;
    this.promptCompressor = promptCompressor;
    this.contentGuardrails = contentGuardrails;
    this.feedbackLearningService = feedbackLearningService;
    this.ragEvaluator = ragEvaluator;
    this.conversationManager = conversationManager;
  }

  /**
   * Generate an answer using the LLM.
   */
  public GenerationResult generate(
      String question,
      String context,
      String conversationHistory,
      List<EmbeddingMatch<TextSegment>> matches,
      String conversationId) {

    String answer;
    int confidenceScore;
    RAGEvaluationMetrics ragMetrics = null;

    if (chatLanguageModel != null && !context.isEmpty()) {
      try {
        // Build and compress prompt
        String prompt = buildPrompt(question, context, conversationHistory);
        if (promptCompressor != null) {
          prompt = promptCompressor.compress(prompt);
        }

        // Check LLM cache first
        String cachedResponse = null;
        if (llmCacheService != null) {
          cachedResponse = llmCacheService.getCachedResponse(prompt);
        }

        if (cachedResponse != null) {
          answer = cachedResponse;
          log.debug("Using cached LLM response");
        } else {
          // Generate with LLM
          answer = chatLanguageModel.generate(prompt);

          // Cache the response
          if (llmCacheService != null) {
            llmCacheService.cacheResponse(prompt, answer);
          }
        }

        // Calculate confidence score
        confidenceScore = calculateConfidenceScore(matches);

        // Apply active learning boost if available
        if (feedbackLearningService != null) {
          double patternSuccessRate = feedbackLearningService.getPatternSuccessRate(question);
          if (patternSuccessRate < 0.5) {
            log.warn("Query pattern has low success rate: {}", patternSuccessRate);
            confidenceScore = (int) (confidenceScore * 0.9);
          }
        }

        // Apply content guardrails
        if (contentGuardrails != null) {
          ContentGuardrails.GuardrailResult guardrailResult = 
              contentGuardrails.validate(answer, confidenceScore);

          if (!guardrailResult.isSafe()) {
            log.warn("Content guardrails detected issues: {}", guardrailResult.getViolations());
            answer = contentGuardrails.sanitize(answer, guardrailResult);
            confidenceScore = Math.min(confidenceScore, guardrailResult.getSafetyScore());
          }
        }

        // Save conversation turn
        if (conversationId != null && conversationManager != null) {
          try {
            conversationManager.addTurn(conversationId, question, answer);
          } catch (Exception e) {
            log.warn("Failed to save conversation turn: {}", e.getMessage());
          }
        }

        // Evaluate RAG quality
        if (ragEvaluator != null) {
          try {
            ragMetrics = ragEvaluator.evaluate(question, answer, matches);
            log.debug("RAG Metrics - Faithfulness: {}, Relevance: {}",
                ragMetrics.getFaithfulness(), ragMetrics.getAnswerRelevance());
          } catch (Exception e) {
            log.warn("RAG evaluation failed: {}", e.getMessage());
          }
        }

      } catch (Exception e) {
        log.error("LLM generation failed: {}", e.getMessage());

        if (isRateLimitError(e)) {
          log.warn("Rate limit exceeded, using fallback response");
          answer = "I'm experiencing high demand right now. Here's what I found:\n\n" + context;
          confidenceScore = Math.min(70, calculateConfidenceScore(matches));
        } else {
          answer = "Based on the available information:\n\n" + context;
          confidenceScore = Math.min(60, calculateConfidenceScore(matches));
        }
      }
    } else if (!context.isEmpty()) {
      answer = "Based on the available information:\n\n" + context;
      confidenceScore = Math.min(70, calculateConfidenceScore(matches));
    } else {
      answer = "I couldn't find relevant information to answer your question. "
          + "Try rephrasing or asking about something else related to your cycles, pitches, or meetings.";
      confidenceScore = 0;
    }

    return GenerationResult.builder()
        .answer(answer)
        .confidenceScore(confidenceScore)
        .ragMetrics(ragMetrics)
        .build();
  }

  /**
   * Build the prompt for the LLM.
   */
  public String buildPrompt(String question, String context, String conversationHistory) {
    StringBuilder prompt = new StringBuilder();

    prompt.append(
        """
            You are a helpful assistant for the ShipFlow application, which helps teams manage their work using the Shape Up methodology.

            Shape Up key concepts:
            - A "Cycle" is a fixed time period (typically 6 weeks) for building features. Cycles have phases: SHAPING, BETTING, BUILD, COOLDOWN.
            - A "Pitch" is a shaped proposal for work to be done in a cycle. Pitches have an appetite (time budget), problem statement, and solution.
            - The "Betting Table" is where stakeholders decide which pitches to bet on for the next cycle.
            - "Appetite" is the maximum time budget for a pitch (e.g., 2 weeks or 6 weeks).
            - "Cooldown" is a period between cycles for fixing bugs, addressing technical debt, and exploring ideas.

            Use the following context to answer the user's question. If the context doesn't contain enough information to fully answer the question, say so and provide what information you can.

            Be concise but thorough. If referencing specific sources, mention them naturally in your response.
            """);

    if (conversationHistory != null && !conversationHistory.isEmpty()) {
      prompt.append("\n").append(conversationHistory).append("\n");
    }

    prompt.append(String.format("""
        Context:
        %s

        Question: %s

        Answer:
        """, context, question));

    return prompt.toString();
  }

  /**
   * Calculate confidence score based on matches.
   */
  public int calculateConfidenceScore(List<EmbeddingMatch<TextSegment>> matches) {
    if (matches.isEmpty())
      return 0;

    double avgScore = matches.stream()
        .mapToDouble(EmbeddingMatch::score)
        .average()
        .orElse(0.0);

    int baseScore = (int) (avgScore * 100);
    int sourceBonus = Math.min(matches.size() * 5, 20);

    return Math.min(100, baseScore + sourceBonus);
  }

  /**
   * Generate suggested follow-up questions.
   */
  public List<String> generateSuggestedFollowUps(
      AskQuestionRequest request,
      List<EmbeddingMatch<TextSegment>> matches) {

    List<String> suggestions = new ArrayList<>();

    if (request.getContextType() != null) {
      switch (request.getContextType().toLowerCase()) {
        case "pitch":
          suggestions.add("What is the current status of this pitch?");
          suggestions.add("Are there any risks associated with this pitch?");
          suggestions.add("What meetings have been held for this pitch?");
          break;
        case "meeting":
          suggestions.add("What decisions were made in this meeting?");
          suggestions.add("What are the action items from this meeting?");
          break;
        case "team":
          suggestions.add("What pitches is this team working on?");
          suggestions.add("How is the team progressing in the current cycle?");
          break;
        case "cycle":
          suggestions.add("What pitches are in this cycle?");
          suggestions.add("How is the cycle progressing overall?");
          suggestions.add("Are there any at-risk pitches in this cycle?");
          break;
      }
    } else {
      suggestions.add("Tell me about the current cycle");
      suggestions.add("What pitches need attention?");
      suggestions.add("Summarize recent team activities");
    }

    return suggestions.subList(0, Math.min(3, suggestions.size()));
  }

  /**
   * Build source citations from matches.
   */
  public List<QAResponse.SourceCitation> buildCitations(List<EmbeddingMatch<TextSegment>> matches) {
    List<QAResponse.SourceCitation> citations = new ArrayList<>();

    for (EmbeddingMatch<TextSegment> match : matches) {
      TextSegment segment = match.embedded();
      if (segment == null)
        continue;

      var metadata = segment.metadata();
      Long knowledgeItemId = null;
      String entityType = null;
      Long entityId = null;
      String title = null;

      if (metadata != null) {
        String kid = metadata.getString("knowledgeItemId");
        if (kid != null) {
          try {
            knowledgeItemId = Long.parseLong(kid);
          } catch (NumberFormatException ignored) {
          }
        }
        entityType = metadata.getString("entityType");
        String eid = metadata.getString("entityId");
        if (eid != null) {
          try {
            entityId = Long.parseLong(eid);
          } catch (NumberFormatException ignored) {
          }
        }
        title = metadata.getString("title");
      }

      String text = segment.text();
      String snippet = text.length() > 200 ? text.substring(0, 200) + "..." : text;

      citations.add(QAResponse.SourceCitation.builder()
          .knowledgeItemId(knowledgeItemId)
          .entityType(entityType)
          .entityId(entityId)
          .title(title)
          .snippet(snippet)
          .relevanceScore(match.score())
          .build());
    }

    return citations;
  }

  /**
   * Build source IDs string from matches.
   */
  public String buildSourceIds(List<EmbeddingMatch<TextSegment>> matches) {
    return matches.stream()
        .map(match -> {
          if (match.embedded() != null && match.embedded().metadata() != null) {
            return match.embedded().metadata().getString("knowledgeItemId");
          }
          return null;
        })
        .filter(id -> id != null)
        .collect(java.util.stream.Collectors.joining(","));
  }

  /**
   * Check if LLM is available.
   */
  public boolean isLLMAvailable() {
    return chatLanguageModel != null;
  }

  /**
   * Detect if the exception is a rate limit error.
   */
  private boolean isRateLimitError(Exception e) {
    String message = e.getMessage();
    if (message == null)
      return false;

    return message.toLowerCase().contains("rate limit")
        || message.toLowerCase().contains("429")
        || message.toLowerCase().contains("too many requests")
        || message.toLowerCase().contains("quota exceeded");
  }

  /**
   * Result of answer generation.
   */
  @Getter
  @Builder
  @AllArgsConstructor
  public static class GenerationResult {
    private final String answer;
    private final int confidenceScore;
    private final RAGEvaluationMetrics ragMetrics;
  }
}
