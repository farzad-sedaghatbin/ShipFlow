package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.config.QAConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.QAInteraction;
import com.github.farzadsedaghatbin.shipflow.entity.enums.QAFeedbackType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;
import com.github.farzadsedaghatbin.shipflow.service.qa.*;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling Q&A interactions using RAG (Retrieval-Augmented
 * Generation).
 */
@Service
@Slf4j
public class QAService {

  @Value("${app.features.qa.enabled:false}")
  private boolean qaEnabled;

  @Value("${app.qa.retrieval.top-k:5}")
  private int topK;

  private final KnowledgeItemRepository knowledgeItemRepository;
  private final QAInteractionRepository qaInteractionRepository;
  private final EmbeddingModel embeddingModel;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final ChatLanguageModel chatLanguageModel;
  private final MessageService messageService;
  private final KnowledgeIngestionService knowledgeIngestionService;
  private final QAConfig qaConfig;
  private final AIConfig aiConfig;
  private final AICacheService cacheService;
  private final RAGEvaluator ragEvaluator;
  private final DocumentReranker documentReranker;
  private final ContextWindowManager contextWindowManager;
  private final ConversationManager conversationManager;
  private final SecurityDocumentFilter securityFilter;
  private final QueryDecomposer queryDecomposer;
  private final FeedbackLearningService feedbackLearningService;
  private final LLMCacheService llmCacheService;
  private final PromptCompressor promptCompressor;
  private final ContentGuardrails contentGuardrails;
  private final CycleRepository cycleRepository;

  @Autowired
  public QAService(KnowledgeItemRepository knowledgeItemRepository, QAInteractionRepository qaInteractionRepository,
      @Autowired(required = false) EmbeddingModel embeddingModel,
      @Autowired(required = false) EmbeddingStore<TextSegment> embeddingStore,
      @Autowired(required = false) ChatLanguageModel chatLanguageModel,
      @Autowired(required = false) KnowledgeIngestionService knowledgeIngestionService,
      @Autowired(required = false) QAConfig qaConfig, @Autowired(required = false) AIConfig aiConfig,
      AICacheService cacheService, @Autowired(required = false) RAGEvaluator ragEvaluator,
      @Autowired(required = false) DocumentReranker documentReranker,
      @Autowired(required = false) ContextWindowManager contextWindowManager,
      @Autowired(required = false) ConversationManager conversationManager,
      @Autowired(required = false) SecurityDocumentFilter securityFilter,
      @Autowired(required = false) QueryDecomposer queryDecomposer,
      @Autowired(required = false) FeedbackLearningService feedbackLearningService,
      @Autowired(required = false) LLMCacheService llmCacheService,
      @Autowired(required = false) PromptCompressor promptCompressor,
      @Autowired(required = false) ContentGuardrails contentGuardrails, MessageService messageService,
      @Autowired(required = false) CycleRepository cycleRepository) {
    this.knowledgeItemRepository = knowledgeItemRepository;
    this.qaInteractionRepository = qaInteractionRepository;
    this.embeddingModel = embeddingModel;
    this.embeddingStore = embeddingStore;
    this.chatLanguageModel = chatLanguageModel;
    this.knowledgeIngestionService = knowledgeIngestionService;
    this.qaConfig = qaConfig;
    this.aiConfig = aiConfig;
    this.cacheService = cacheService;
    this.queryDecomposer = queryDecomposer;
    this.feedbackLearningService = feedbackLearningService;
    this.llmCacheService = llmCacheService;
    this.promptCompressor = promptCompressor;
    this.contentGuardrails = contentGuardrails;
    this.ragEvaluator = ragEvaluator;
    this.documentReranker = documentReranker;
    this.contextWindowManager = contextWindowManager;
    this.conversationManager = conversationManager;
    this.securityFilter = securityFilter;
    this.messageService = messageService;
    this.cycleRepository = cycleRepository;
  }

  /**
   * Ask a question and get an AI-generated answer based on retrieved knowledge.
   */
  public QAResponse askQuestion(AskQuestionRequest request, Long userId) {
    long startTime = System.currentTimeMillis();

    if (!isQAEnabled()) {
      return QAResponse.builder().question(request.getQuestion()).aiEnabled(false)
          .errorMessage("Q&A feature is not enabled").answeredAt(LocalDateTime.now()).build();
    }

    // Get or create conversation context early — must happen before the cache
    // check so that every response (including cached ones) carries a
    // conversationId, enabling the client to continue a multi-turn session even
    // when the first answer was served from cache.
    boolean isMultiTurn = request.getConversationId() != null;
    ConversationContext conversation = null;
    if (conversationManager != null) {
      try {
        conversation = conversationManager.getOrCreateConversation(request.getConversationId(), userId,
            request.getContextType(), request.getContextId(), request.getContextName());
      } catch (Exception e) {
        log.warn("Failed to get/create conversation, continuing without history: {}", e.getMessage());
        // Continue without conversation memory - graceful degradation
      }
    }

    // Check cache for similar single-turn questions (skip for multi-turn sessions
    // whose answers incorporate conversation history not captured in the cache key).
    if (!isMultiTurn) {
      Optional<QAResponse> cachedResponse = cacheService.getCachedQAResponse(request.getQuestion(),
          request.getContextType(), request.getContextId(), request.getCycleId(), request.getTeamId());

      if (cachedResponse.isPresent()) {
        log.debug("Returning cached Q&A response for similar question");
        QAResponse cached = cachedResponse.get();
        // Include the newly-created conversationId so the client can continue as
        // a multi-turn session from this cached first answer.
        return QAResponse.builder().question(request.getQuestion()).answer(cached.getAnswer())
            .sources(cached.getSources()).confidenceScore(cached.getConfidenceScore())
            .answeredAt(LocalDateTime.now()).processingTimeMs(System.currentTimeMillis() - startTime)
            .aiEnabled(cached.getAiEnabled()).suggestedFollowUps(cached.getSuggestedFollowUps())
            .cached(true)
            .conversationId(conversation != null ? conversation.getConversationId() : null).build();
      }
    }

    // Entity resolution: extract contextId from question text when not provided
    // Supports both numeric IDs ("cycle 6") and name-based lookup ("Build Season
    // cycle")
    if (request.getContextId() == null && request.getContextType() != null) {
      resolveEntityFromQuestion(request);
    }

    // Evolve conversation context whenever entity resolution just resolved a new
    // entity mid-conversation (e.g. user asks about a different cycle).
    if (conversation != null && conversationManager != null && request.getContextId() != null) {
      try {
        conversationManager.updateContext(conversation.getConversationId(),
            request.getContextType(), request.getContextId(), request.getContextName());
      } catch (Exception e) {
        log.debug("Could not update conversation context: {}", e.getMessage());
      }
    }

    // Try to infer context from conversation history if not provided
    if (conversation != null && conversationManager != null && request.getContextId() == null
        && request.getContextType() != null) {
      try {
        ConversationContext.ContextInfo previousContext = conversationManager
            .getMostRecentContext(conversation.getConversationId());
        if (previousContext != null
            && previousContext.getContextType().equalsIgnoreCase(request.getContextType())) {
          request.setContextId(previousContext.getContextId());
          if (previousContext.getContextName() != null && request.getContextName() == null) {
            request.setContextName(previousContext.getContextName());
          }
          log.info("Inferred contextId={}, contextName='{}' from conversation history",
              previousContext.getContextId(), previousContext.getContextName());
        }
      } catch (Exception e) {
        log.debug("Could not infer context from conversation: {}", e.getMessage());
      }
    }

    // Check for ambiguous contextual references
    String ambiguityCheck = checkForAmbiguousContext(request);
    if (ambiguityCheck != null) {
      log.info("Detected ambiguous context in question: {}", request.getQuestion());
      // Save the clarification exchange as a conversation turn so the next question
      // has history of what was asked and what was asked back.
      if (conversation != null && conversationManager != null) {
        try {
          conversationManager.addTurn(conversation.getConversationId(),
              request.getQuestion(), ambiguityCheck);
        } catch (Exception e) {
          log.debug("Could not save clarification turn: {}", e.getMessage());
        }
      }
      return QAResponse.builder().question(request.getQuestion()).answer(ambiguityCheck).confidenceScore(0)
          .answeredAt(LocalDateTime.now()).processingTimeMs(System.currentTimeMillis() - startTime)
          .aiEnabled(true).suggestedFollowUps(generateClarificationSuggestions(request)).cached(false)
          .conversationId(conversation != null ? conversation.getConversationId() : null).build();
    }

    String originalQuestion = request.getQuestion();

    try {
      // 0. Query decomposition for complex questions
      List<String> subQueries = List.of(request.getQuestion());
      if (queryDecomposer != null && queryDecomposer.shouldDecompose(request.getQuestion())) {
        try {
          QueryDecomposer.DecompositionResult decomposition = queryDecomposer
              .decomposeWithMetadata(request.getQuestion());
          if (decomposition.wasDecomposed()) {
            subQueries = decomposition.getSubQueries();
            log.info("Decomposed query into {} sub-queries", subQueries.size());
          }
        } catch (Exception e) {
          log.warn("Query decomposition failed, using original query: {}", e.getMessage());
        }
      }

      // 2. Embed the question with retry logic
      Embedding questionEmbedding;
      try {
        questionEmbedding = embeddingModel.embed(request.getQuestion()).content();
      } catch (Exception e) {
        log.error("Embedding service failed: {}", e.getMessage(), e);
        throw new RuntimeException(messageService.getMessage("error.qa.embedding.unavailable"), e);
      }

      // 3. Retrieve relevant knowledge chunks with fallback
      int retrieveK = qaConfig != null ? qaConfig.getTopK() : topK;
      double minScore = 0.70; // Increased threshold for better quality
      List<EmbeddingMatch<TextSegment>> matches;

      try {
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
            .queryEmbedding(questionEmbedding).maxResults(retrieveK * 2) // Retrieve more, then filter
            .minScore(minScore).build();

        EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
        matches = searchResult.matches();

        log.debug("Vector store returned {} matches", matches.size());
      } catch (Exception e) {
        log.error("Vector store search failed, attempting fallback: {}", e.getMessage());

        // Fallback: Try basic text search or return cached response
        matches = attemptFallbackSearch(request);

        if (matches == null || matches.isEmpty()) {
          // Last resort: check cache for similar questions
          Optional<QAResponse> fallbackCachedResponse = cacheService.getCachedQAResponse(
              request.getQuestion(), request.getContextType(), request.getContextId(),
              request.getCycleId(), request.getTeamId());

          if (fallbackCachedResponse.isPresent()) {
            log.info("Returning cached response due to vector store failure");
            QAResponse cached = fallbackCachedResponse.get();
            cached.setCached(true);
            return cached;
          }

          throw new RuntimeException(messageService.getMessage("error.qa.vector.store.unavailable"), e);
        }
      }

      // 3. Apply security filtering
      if (securityFilter != null) {
        // TODO: Get user's team and project IDs from UserService
        // For now, skip security filtering
        // matches = securityFilter.filterByUserAccess(matches, userId, userTeamIds,
        // userProjectIds);
      }

      // 4. Filter by context
      if (request.getCycleId() != null || request.getTeamId() != null
          || request.getContextId() != null || request.getContextName() != null) {
        matches = filterMatchesByContext(matches, request);
      }

      // 5. Filter by minimum relevance
      matches = matches.stream().filter(match -> match.score() >= minScore).collect(Collectors.toList());

      // 5.5. Apply recency boost - favor more recent documents
      matches = applyRecencyBoost(matches);

      // 6. Re-rank for better ordering
      if (documentReranker != null && matches.size() > retrieveK) {
        matches = documentReranker.rerank(request.getQuestion(), matches, retrieveK);
      } else {
        matches = matches.stream().limit(retrieveK).collect(Collectors.toList());
      }

      // Log retrieval metrics
      log.debug("Retrieved {} documents with avg score: {}", matches.size(),
          matches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0));

      // 7. Build context with token management
      String context;
      if (contextWindowManager != null) {
        ContextWindowManager.ContextResult contextResult = contextWindowManager.buildManagedContext(matches,
            request.getQuestion(), 4000);
        context = contextResult.getContext();
      } else {
        context = buildContext(matches);
      }

      List<QAResponse.SourceCitation> citations = buildCitations(matches);

      // 8. Add conversation history if available
      String conversationHistory = "";
      if (conversation != null && conversationManager != null) {
        conversationHistory = conversationManager.buildConversationHistory(conversation.getConversationId(), 3);
      }

      // 9. Generate answer using LLM with rate limit handling, caching, compression,
      // and guardrails
      String answer;
      int confidenceScore;
      RAGEvaluationMetrics ragMetrics = null;

      if (chatLanguageModel != null && !context.isEmpty()) {
        try {
          // Build and compress prompt
          String prompt = buildPrompt(request.getQuestion(), context, conversationHistory,
              request.getContextType(), request.getContextName(), request.getContextId());
          if (promptCompressor != null) {
            prompt = promptCompressor.compress(prompt);
          }

          // Check LLM cache first
          String cachedLLMResponse = null;
          if (llmCacheService != null) {
            cachedLLMResponse = llmCacheService.getCachedResponse(prompt);
          }

          if (cachedLLMResponse != null) {
            answer = cachedLLMResponse;
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
            double patternSuccessRate = feedbackLearningService
                .getPatternSuccessRate(request.getQuestion());
            if (patternSuccessRate < 0.5) {
              log.warn("Query pattern has low success rate: {}", patternSuccessRate);
              confidenceScore = (int) (confidenceScore * 0.9); // Reduce confidence
            }
          }

          // Apply content guardrails
          if (contentGuardrails != null) {
            ContentGuardrails.GuardrailResult guardrailResult = contentGuardrails.validate(answer,
                confidenceScore);

            if (!guardrailResult.isSafe()) {
              log.warn("Content guardrails detected issues: {}", guardrailResult.getViolations());
              answer = contentGuardrails.sanitize(answer, guardrailResult);
              confidenceScore = Math.min(confidenceScore, guardrailResult.getSafetyScore());
            }
          }

          // Save conversation turn
          if (conversation != null && conversationManager != null) {
            try {
              conversationManager.addTurn(conversation.getConversationId(), request.getQuestion(),
                  answer);
            } catch (Exception e) {
              log.warn("Failed to save conversation turn: {}", e.getMessage());
              // Continue - this is not critical
            }
          }

          // Evaluate RAG quality
          if (ragEvaluator != null) {
            try {
              ragMetrics = ragEvaluator.evaluate(request.getQuestion(), answer, matches);
              log.debug("RAG Metrics - Faithfulness: {}, Relevance: {}", ragMetrics.getFaithfulness(),
                  ragMetrics.getAnswerRelevance());
            } catch (Exception e) {
              log.warn("RAG evaluation failed: {}", e.getMessage());
              // Continue - evaluation is optional
            }
          }
        } catch (Exception e) {
          log.error("LLM generation failed: {}", e.getMessage());

          // Check if it's a rate limit error
          if (isRateLimitError(e)) {
            log.warn("Rate limit exceeded, using fallback response");
            answer = "I'm experiencing high demand right now. Here's what I found:\n\n" + context;
            confidenceScore = Math.min(70, calculateConfidenceScore(matches));
          } else {
            // Other LLM errors - return context directly
            answer = "Based on the available information:\n\n" + context;
            confidenceScore = Math.min(60, calculateConfidenceScore(matches));
          }
        }
      } else if (!context.isEmpty()) {
        // Fallback: return context directly if no LLM
        answer = "Based on the available information:\n\n" + context;
        confidenceScore = Math.min(70, calculateConfidenceScore(matches));
      } else {
        // No semantic matches found - try database keyword search as fallback
        log.info("No semantic matches found, attempting database keyword search for: {}",
            request.getQuestion());
        List<EmbeddingMatch<TextSegment>> databaseMatches = searchDatabaseByKeywords(request.getQuestion(),
            request.getContextType(), request.getContextId());

        if (!databaseMatches.isEmpty()) {
          log.info("Found {} database matches", databaseMatches.size());
          matches = databaseMatches;
          context = buildContext(matches);

          if (chatLanguageModel != null && !context.isEmpty()) {
            try {
              // Build and compress prompt
              String prompt = buildPrompt(request.getQuestion(), context, conversationHistory,
                  request.getContextType(), request.getContextName(), request.getContextId());
              if (promptCompressor != null) {
                prompt = promptCompressor.compress(prompt);
              }

              // Generate with LLM
              answer = chatLanguageModel.generate(prompt);
              confidenceScore = Math.min(75, calculateConfidenceScore(matches));
            } catch (Exception e) {
              log.error("Error generating LLM answer for keyword search results", e);
              answer = "Based on the available information:\n\n" + context;
              confidenceScore = Math.min(65, calculateConfidenceScore(matches));
            }
          } else {
            answer = "Based on the available information:\n\n" + context;
            confidenceScore = Math.min(65, calculateConfidenceScore(matches));
          }
        } else {
          answer = "I couldn't find relevant information to answer your question. "
              + "Try rephrasing or asking about something else related to your cycles, pitches, or meetings.";
          confidenceScore = 0;
        }
      }

      // 6. Save the interaction
      QAInteraction interaction = QAInteraction.builder().question(originalQuestion) // Save original question
          .answer(answer).contextType(request.getContextType()).contextId(request.getContextId())
          .cycleId(request.getCycleId()).teamId(request.getTeamId()).userId(userId)
          .sourceKnowledgeIds(buildSourceIds(matches)).confidenceScore(confidenceScore)
          .processingTimeMs(System.currentTimeMillis() - startTime).build();

      Long interactionId = null;
      try {
        interaction = saveInteraction(interaction);
        interactionId = interaction.getId();
      } catch (Exception e) {
        log.error("Failed to save QA interaction: {}", e.getMessage(), e);
        // Continue without saving - the user still gets their answer
      }

      // 11. Build response
      QAResponse response = QAResponse.builder().interactionId(interactionId).question(originalQuestion) // Use
          // original
          // question
          // in
          // response
          .answer(answer).sources(request.getIncludeSources() ? citations : null)
          .confidenceScore(confidenceScore).answeredAt(LocalDateTime.now())
          .processingTimeMs(System.currentTimeMillis() - startTime).aiEnabled(chatLanguageModel != null)
          .suggestedFollowUps(generateSuggestedFollowUps(request, matches)).cached(false)
          .conversationId(conversation != null ? conversation.getConversationId() : null).build();

      // 8. Cache the response for future similar questions (skip for multi-turn
      // conversations whose answers incorporate conversation history).
      if (!isMultiTurn) {
        cacheService.cacheQAResponse(originalQuestion,
            request.getContextType(), request.getContextId(), request.getCycleId(), request.getTeamId(),
            response);
      }

      return response;

    } catch (Exception e) {
      log.error("Error processing question: {}", e.getMessage(), e);

      return QAResponse.builder().question(request.getQuestion()).aiEnabled(chatLanguageModel != null)
          .errorMessage("Failed to process question: " + e.getMessage()).answeredAt(LocalDateTime.now())
          .processingTimeMs(System.currentTimeMillis() - startTime).build();
    }
  }

  /** Submit feedback for a Q&A response. */
  @Transactional
  public void submitFeedback(QAFeedbackRequest request, Long userId) {
    QAInteraction interaction = qaInteractionRepository.findById(request.getInteractionId())
        .orElseThrow(() -> new RuntimeException("Q&A interaction not found: " + request.getInteractionId()));

    // Verify the user owns this interaction
    if (!interaction.getUserId().equals(userId)) {
      throw new RuntimeException(messageService.getMessage("error.qa.feedback.not.owner"));
    }

    interaction.setFeedbackType(request.getFeedbackType());
    interaction.setFeedbackAt(LocalDateTime.now());

    if (request.getFeedbackType() == QAFeedbackType.CORRECTED && request.getCorrection() != null) {
      interaction.setFeedbackCorrection(request.getCorrection());
    }

    qaInteractionRepository.save(interaction);

    // If validated, ingest as knowledge
    if (request.getFeedbackType() == QAFeedbackType.ACCURATE
        || request.getFeedbackType() == QAFeedbackType.CORRECTED) {
      if (knowledgeIngestionService != null) {
        knowledgeIngestionService.ingestValidatedQA(interaction.getId());
      }
    }

    log.info("Feedback submitted for interaction {}: {}", interaction.getId(), request.getFeedbackType());
  }

  /** Record simple helpful/unhelpful feedback for active learning. */
  @Transactional
  public void recordSimpleFeedback(Long interactionId, boolean helpful, String text) {
    if (feedbackLearningService != null) {
      feedbackLearningService.recordFeedback(interactionId, helpful, text);
      log.info("Active learning feedback recorded for interaction {}: helpful={}", interactionId, helpful);
    } else {
      log.warn("FeedbackLearningService not available, feedback not recorded");
    }
  }

  /** Get the status of the Q&A feature. */
  public QAStatusDTO getStatus() {
    return QAStatusDTO.builder().qaEnabled(isQAEnabled()).aiAvailable(chatLanguageModel != null)
        .vectorStoreType(qaConfig != null ? qaConfig.getVectorStoreProvider() : "none")
        .totalKnowledgeItems(knowledgeItemRepository.count())
        .embeddedKnowledgeItems(knowledgeItemRepository.countEmbedded())
        .totalInteractions(qaInteractionRepository.count())
        .validatedInteractions(qaInteractionRepository
            .countValidated(List.of(QAFeedbackType.ACCURATE, QAFeedbackType.CORRECTED)))
        .embeddingModel("all-MiniLM-L6-v2").llmModel(aiConfig != null ? aiConfig.getModelName() : "none")
        .build();
  }

  /** Get recent interactions for a user. */
  public List<QAInteraction> getRecentInteractions(Long userId) {
    return qaInteractionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
  }

  /**
   * Save QA interaction. Note: @Transactional removed because it has no effect on
   * private methods
   * called internally (Spring AOP proxy limitation). The repository save already
   * participates
   * in any existing transaction from the caller if present.
   */
  private QAInteraction saveInteraction(QAInteraction interaction) {
    return qaInteractionRepository.save(interaction);
  }

  // ===== Private helper methods =====

  private boolean isQAEnabled() {
    return qaEnabled && embeddingModel != null && embeddingStore != null;
  }

  private List<EmbeddingMatch<TextSegment>> filterMatchesByContext(List<EmbeddingMatch<TextSegment>> matches,
      AskQuestionRequest request) {

    return matches.stream().filter(match -> {
      TextSegment segment = match.embedded();
      if (segment == null || segment.metadata() == null)
        return true;

      // Filter by cycle
      if (request.getCycleId() != null) {
        String cycleId = segment.metadata().getString("cycleId");
        if (cycleId != null && !cycleId.equals(request.getCycleId().toString())) {
          return false;
        }
      }

      // Filter by team
      if (request.getTeamId() != null) {
        String teamId = segment.metadata().getString("teamId");
        if (teamId != null && !teamId.equals(request.getTeamId().toString())) {
          return false;
        }
      }

      // Filter by cycle name when cycleId is not available but contextName is
      if (request.getCycleId() == null && request.getContextName() != null
          && "cycle".equalsIgnoreCase(request.getContextType())) {
        String cycleName = segment.metadata().getString("cycleName");
        if (cycleName != null && !cycleName.toLowerCase().contains(request.getContextName().toLowerCase())) {
          return false;
        }
      }

      // Filter by specific entity context (prioritize exact matches but allow related
      // docs)
      if (request.getContextType() != null && request.getContextId() != null) {
        String entityType = segment.metadata().getString("entityType");
        String entityId = segment.metadata().getString("entityId");
        // Keep docs that match exactly OR belong to the same cycle/team
        if (request.getContextType().equalsIgnoreCase(entityType)
            && request.getContextId().toString().equals(entityId)) {
          return true; // exact match — always keep
        }
        // Related entities (same cycle) are kept by the cycleId filter above
      }

      return true;
    }).collect(Collectors.toList());
  }

  /**
   * Apply recency boost to search results. More recently updated documents
   * get a higher effective score, helping surface the most current information.
   * 
   * Boost factors:
   * - Updated within 7 days: +15% boost
   * - Updated within 30 days: +10% boost
   * - Updated within 90 days: +5% boost
   * - Older: no boost
   */
  private List<EmbeddingMatch<TextSegment>> applyRecencyBoost(List<EmbeddingMatch<TextSegment>> matches) {
    if (matches.isEmpty()) {
      return matches;
    }

    LocalDateTime now = LocalDateTime.now();
    LocalDateTime sevenDaysAgo = now.minusDays(7);
    LocalDateTime thirtyDaysAgo = now.minusDays(30);
    LocalDateTime ninetyDaysAgo = now.minusDays(90);

    return matches.stream()
        .map(match -> {
          double boostFactor = 1.0;

          if (match.embedded() != null && match.embedded().metadata() != null) {
            String updatedAtStr = match.embedded().metadata().getString("updatedAt");
            if (updatedAtStr != null) {
              try {
                LocalDateTime updatedAt = LocalDateTime.parse(updatedAtStr);
                if (updatedAt.isAfter(sevenDaysAgo)) {
                  boostFactor = 1.15; // 15% boost for very recent
                } else if (updatedAt.isAfter(thirtyDaysAgo)) {
                  boostFactor = 1.10; // 10% boost for recent
                } else if (updatedAt.isAfter(ninetyDaysAgo)) {
                  boostFactor = 1.05; // 5% boost for somewhat recent
                }
              } catch (Exception e) {
                // Invalid date format, no boost applied
                log.debug("Could not parse updatedAt for recency boost: {}", updatedAtStr);
              }
            }
          }

          // Create new match with boosted score (capped at 1.0)
          double boostedScore = Math.min(1.0, match.score() * boostFactor);
          return new EmbeddingMatch<>(boostedScore, match.embeddingId(), match.embedding(), match.embedded());
        })
        .sorted((a, b) -> Double.compare(b.score(), a.score())) // Re-sort by boosted scores
        .collect(Collectors.toList());
  }

  private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
    if (matches.isEmpty())
      return "";

    StringBuilder context = new StringBuilder();
    for (int i = 0; i < matches.size(); i++) {
      EmbeddingMatch<TextSegment> match = matches.get(i);
      TextSegment segment = match.embedded();

      String title = segment.metadata() != null ? segment.metadata().getString("title") : "Unknown";

      context.append("[Source ").append(i + 1).append(": ").append(title).append("]\n");
      context.append(segment.text()).append("\n\n");
    }

    return context.toString();
  }

  private List<QAResponse.SourceCitation> buildCitations(List<EmbeddingMatch<TextSegment>> matches) {
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

      // Create snippet (first 200 chars)
      String text = segment.text();
      String snippet = text.length() > 200 ? text.substring(0, 200) + "..." : text;

      citations.add(QAResponse.SourceCitation.builder().knowledgeItemId(knowledgeItemId).entityType(entityType)
          .entityId(entityId).title(title).snippet(snippet).relevanceScore(match.score()).build());
    }

    return citations;
  }

  private String buildSourceIds(List<EmbeddingMatch<TextSegment>> matches) {
    return matches.stream().map(match -> {
      if (match.embedded() != null && match.embedded().metadata() != null) {
        return match.embedded().metadata().getString("knowledgeItemId");
      }
      return null;
    }).filter(id -> id != null).collect(Collectors.joining(","));
  }

  private String buildPrompt(String question, String context, String conversationHistory,
      String contextType, String contextName, Long contextId) {
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

    // Add entity context header if available
    if (contextType != null && (contextName != null || contextId != null)) {
      prompt.append("\nYou are answering about: ").append(contextType);
      if (contextName != null) {
        prompt.append(" '").append(contextName).append("'");
      }
      if (contextId != null) {
        prompt.append(" (ID: ").append(contextId).append(")");
      }
      prompt.append(".\n");
    }

    // Add conversation history if available
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

  private int calculateConfidenceScore(List<EmbeddingMatch<TextSegment>> matches) {
    if (matches.isEmpty())
      return 0;

    // Calculate average similarity score
    double avgScore = matches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0.0);

    // Scale to 0-100, with bonus for having multiple relevant sources
    int baseScore = (int) (avgScore * 100);
    int sourceBonus = Math.min(matches.size() * 5, 20);

    return Math.min(100, baseScore + sourceBonus);
  }

  private List<String> generateSuggestedFollowUps(AskQuestionRequest request,
      List<EmbeddingMatch<TextSegment>> matches) {
    List<String> suggestions = new ArrayList<>();

    // Generate context-aware follow-up suggestions
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
   * Attempt fallback search when vector store fails. This could use a backup
   * vector store or basic text matching.
   */
  private List<EmbeddingMatch<TextSegment>> attemptFallbackSearch(AskQuestionRequest request) {
    try {
      // TODO: Implement fallback search strategy
      // Options:
      // 1. Use Elasticsearch for text-based search
      // 2. Use a backup vector store
      // 3. Use cached similar questions

      log.warn("Fallback search not yet implemented, returning empty results");
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Fallback search also failed: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /** Detect if the exception is a rate limit error from the LLM provider. */
  private boolean isRateLimitError(Exception e) {
    String message = e.getMessage();
    if (message == null)
      return false;

    // Check for common rate limit indicators
    return message.toLowerCase().contains("rate limit") || message.toLowerCase().contains("429")
        || message.toLowerCase().contains("too many requests")
        || message.toLowerCase().contains("quota exceeded");
  }

  /**
   * Search database for knowledge items using keyword matching when semantic
   * search fails. This provides a fallback for short queries like "cycle 4" that
   * don't embed well.
   */
  private List<EmbeddingMatch<TextSegment>> searchDatabaseByKeywords(String question, String contextType,
      Long contextId) {
    try {
      if (question == null || question.trim().isEmpty()) {
        return Collections.emptyList();
      }

      // Extract meaningful search terms from the question
      List<String> searchTerms = extractSearchTerms(question);
      log.info("Extracted search terms from question: {}", searchTerms);

      List<com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem> items;

      // Get total knowledge items for debugging
      List<com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem> allItems = knowledgeItemRepository
          .findAll();
      long embeddedCount = allItems.stream().filter(item -> item.getIsEmbedded()).count();
      log.info(
          "Database keyword search: total items={}, embedded items={}, search terms={}, contextType={}, contextId={}",
          allItems.size(), embeddedCount, searchTerms, contextType, contextId);

      // Log entity type distribution
      Map<String, Long> entityTypeCounts = allItems.stream().filter(item -> item.getIsEmbedded())
          .collect(Collectors.groupingBy(item -> item.getEntityType().name(), Collectors.counting()));
      log.info("Entity type distribution: {}", entityTypeCounts);

      // Log sample items matching contextType if specified
      if (contextType != null) {
        log.info("Looking for items with contextType={}", contextType);
        long matchingTypeCount = allItems.stream().filter(
            item -> item.getIsEmbedded() && item.getEntityType().name().equalsIgnoreCase(contextType))
            .count();
        log.info("Found {} items with entityType={}", matchingTypeCount, contextType);

        if (matchingTypeCount > 0) {
          allItems.stream().filter(
              item -> item.getIsEmbedded() && item.getEntityType().name().equalsIgnoreCase(contextType))
              .limit(3).forEach(item -> {
                String contentPreview = item.getContent() != null && item.getContent().length() > 100
                    ? item.getContent().substring(0, 100) + "..."
                    : item.getContent();
                log.info("Sample {} item: entityId={}, title='{}', content preview='{}'",
                    item.getEntityType().name(), item.getEntityId(), item.getTitle(),
                    contentPreview);
              });
        } else {
          log.info("No items found with entityType={}, will search across all entity types", contextType);
        }
      }

      // Search based on context
      // Check if the contextType actually has items in the database
      boolean contextTypeHasItems = contextType == null || allItems.stream().anyMatch(
          item -> item.getIsEmbedded() && item.getEntityType().name().equalsIgnoreCase(contextType));

      if (contextType != null && contextId != null) {
        // Search within specific context
        items = knowledgeItemRepository.findAll().stream().filter(item -> item.getIsEmbedded()).filter(item -> {
          String content = (item.getContent() + " " + (item.getTitle() != null ? item.getTitle() : ""))
              .toLowerCase();
          boolean matchesSearch = searchTerms.stream().anyMatch(term -> content.contains(term));
          boolean matchesContext = contextType.equalsIgnoreCase(item.getEntityType().name())
              && contextId.equals(item.getEntityId());
          return matchesSearch || matchesContext;
        }).limit(5).collect(Collectors.toList());
      } else if (contextType != null && contextTypeHasItems) {
        // Search within entity type (only if that type exists)
        items = knowledgeItemRepository.findAll().stream().filter(item -> item.getIsEmbedded()).filter(item -> {
          String content = (item.getContent() + " " + (item.getTitle() != null ? item.getTitle() : ""))
              .toLowerCase();
          return searchTerms.stream().anyMatch(term -> content.contains(term))
              && contextType.equalsIgnoreCase(item.getEntityType().name());
        }).limit(5).collect(Collectors.toList());
      } else {
        // General keyword search
        items = knowledgeItemRepository.findAll().stream().filter(item -> item.getIsEmbedded()).filter(item -> {
          String content = (item.getContent() + " " + (item.getTitle() != null ? item.getTitle() : ""))
              .toLowerCase();
          return searchTerms.stream().anyMatch(term -> content.contains(term));
        }).limit(5).collect(Collectors.toList());
      }

      log.info("Database keyword search found {} items matching any of: {}", items.size(), searchTerms);
      if (!items.isEmpty()) {
        log.debug("Sample matched item: entityType={}, entityId={}, title={}", items.get(0).getEntityType(),
            items.get(0).getEntityId(), items.get(0).getTitle());
      }

      // Convert to EmbeddingMatch format for compatibility
      return items.stream().map(item -> {
        TextSegment segment = TextSegment.from(item.getContent(),
            new dev.langchain4j.data.document.Metadata().put("id", item.getId().toString())
                .put("entityType", item.getEntityType().name())
                .put("entityId", item.getEntityId().toString())
                .put("title", item.getTitle() != null ? item.getTitle() : ""));
        return new EmbeddingMatch<>(0.75, // Fixed relevance score for keyword matches
            item.getEmbeddingId(), null, // No embedding for keyword match
            segment);
      }).collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Database keyword search failed: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Extract meaningful search terms from a question. Examples: - "Are there any
   * at-risk pitches in cycle 5?" → ["cycle 5", "at-risk", "at_risk", "risk"] -
   * "cycle 4" → ["cycle 4", "4"] - "What is the status of the API pitch?" →
   * ["api", "status", "pitch"]
   */
  private List<String> extractSearchTerms(String question) {
    if (question == null || question.trim().isEmpty()) {
      return Collections.emptyList();
    }

    List<String> terms = new ArrayList<>();
    String lowerQuestion = question.toLowerCase().trim();

    // Extract entity + number patterns (cycle 5, pitch 4, etc.)
    java.util.regex.Pattern entityPattern = java.util.regex.Pattern
        .compile("(cycle|pitch|team|meeting|initiative|epic|release)\\s+\\d+");
    java.util.regex.Matcher entityMatcher = entityPattern.matcher(lowerQuestion);
    while (entityMatcher.find()) {
      terms.add(entityMatcher.group().trim());
    }

    // Extract and convert status-related terms
    // "at-risk" → both "at-risk" and "at_risk" (for enum matching)
    if (lowerQuestion.contains("at-risk") || lowerQuestion.contains("at risk") || lowerQuestion.contains("risk")) {
      terms.add("at-risk");
      terms.add("at_risk");
      terms.add("risk");
    }

    // "in progress" → both forms
    if (lowerQuestion.contains("in progress") || lowerQuestion.contains("in-progress")
        || lowerQuestion.contains("progress")) {
      terms.add("in_progress");
      terms.add("in progress");
      terms.add("progress");
    }

    // Other status terms
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

    // Roadmap-specific status terms
    if (lowerQuestion.contains("draft")) {
      terms.add("draft");
    }

    if (lowerQuestion.contains("active") || lowerQuestion.contains("ongoing")) {
      terms.add("active");
      terms.add("ongoing");
    }

    if (lowerQuestion.contains("on hold") || lowerQuestion.contains("on-hold")
        || lowerQuestion.contains("onhold")) {
      terms.add("on_hold");
      terms.add("on hold");
    }

    if (lowerQuestion.contains("planned") || lowerQuestion.contains("planning")) {
      terms.add("planned");
      terms.add("planning");
    }

    if (lowerQuestion.contains("cancelled") || lowerQuestion.contains("canceled")) {
      terms.add("cancelled");
      terms.add("canceled");
    }

    if (lowerQuestion.contains("released")) {
      terms.add("released");
    }

    if (lowerQuestion.contains("testing")) {
      terms.add("testing");
    }

    // Extract hyphenated words (using possessive quantifiers to prevent ReDoS)
    java.util.regex.Pattern hyphenPattern = java.util.regex.Pattern.compile("\\b\\w++-\\w++\\b");
    java.util.regex.Matcher hyphenMatcher = hyphenPattern.matcher(lowerQuestion);
    while (hyphenMatcher.find()) {
      String hyphenated = hyphenMatcher.group().trim();
      terms.add(hyphenated);
      // Also add underscore version for enum matching
      terms.add(hyphenated.replace("-", "_"));
    }

    // Remove common question words
    String cleaned = lowerQuestion.replaceAll(
        "\\b(what|where|when|who|why|how|is|are|the|a|an|in|on|at|for|to|of|about|tell|me|show|any|there)\\b",
        " ").replaceAll("\\s+", " ").trim();

    // Extract significant words (3+ characters, not stop words)
    String[] words = cleaned.split("\\s+");
    for (String word : words) {
      if (word.length() >= 3 && !word.matches("\\d+")) {
        terms.add(word);
      }
    }

    // Add standalone numbers (could be cycle/pitch IDs)
    java.util.regex.Pattern numberPattern = java.util.regex.Pattern.compile("\\b\\d+\\b");
    java.util.regex.Matcher numberMatcher = numberPattern.matcher(lowerQuestion);
    while (numberMatcher.find()) {
      terms.add(numberMatcher.group());
    }

    // Remove duplicates and empty strings
    return terms.stream().filter(t -> t != null && !t.trim().isEmpty()).distinct().collect(Collectors.toList());
  }

  /**
   * Resolve entity context (contextId, contextName, cycleId) from the question
   * text.
   * Tries numeric ID extraction first, then falls back to name-based DB lookup.
   * Only runs when contextId is null and contextType is set.
   */
  private void resolveEntityFromQuestion(AskQuestionRequest request) {
    String question = request.getQuestion();
    String contextType = request.getContextType();

    // For cycles: run name-based lookup first (or in parallel with numeric lookup)
    // so that "Cycle 5" resolves to the cycle *named* "Cycle 5" rather than the
    // cycle whose database ID happens to be 5.
    if ("cycle".equalsIgnoreCase(contextType) && cycleRepository != null) {
      // Prefer a name match — try extracting a name token from the question.
      // extractEntityNameFromQuestion skips pure-numeric tokens, so "cycle 5" won't
      // match here when used alone. We also try the composite string "Cycle <N>"
      // explicitly so users who named their cycle "Cycle 5" get the right result.
      Long numericId = extractEntityIdFromQuestion(question, contextType);
      String candidateName = null;
      if (numericId != null) {
        // Build the composite name that the user is most likely referring to,
        // e.g. the question "what's in cycle 5?" → candidate name "Cycle 5".
        candidateName = contextType.substring(0, 1).toUpperCase()
            + contextType.substring(1).toLowerCase() + " " + numericId;
      } else {
        candidateName = extractEntityNameFromQuestion(question, contextType);
      }

      if (candidateName != null) {
        try {
          List<Cycle> nameMatches = cycleRepository.findByNameContainingIgnoreCase(candidateName);

          // Prefer an exact (case-insensitive) match over a partial one to avoid
          // "Cycle 5" accidentally resolving to "Cycle 50".
          final String finalCandidateName = candidateName;
          List<Cycle> exactMatches = nameMatches.stream()
              .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(finalCandidateName))
              .collect(Collectors.toList());

          if (exactMatches.size() == 1) {
            Cycle cycle = exactMatches.get(0);
            request.setContextId(cycle.getId());
            if (request.getCycleId() == null) {
              request.setCycleId(cycle.getId());
            }
            request.setContextName(cycle.getName());
            log.info("Resolved cycle by exact name '{}' → ID={}, name='{}'",
                candidateName, cycle.getId(), cycle.getName());
            return;
          } else if (exactMatches.size() > 1) {
            request.setContextName(candidateName);
            log.info("Multiple cycles exactly match name '{}' ({}), using name-based vector filtering",
                candidateName, exactMatches.size());
            return;
          } else if (nameMatches.size() > 1) {
            // Multiple partial matches — use name as vector-search filter; don't pin a
            // single ID.
            request.setContextName(candidateName);
            log.info("Multiple partial cycle name matches for '{}' ({}), using name-based vector filtering",
                candidateName, nameMatches.size());
            return;
          }
          // No match at all — fall through to numeric ID lookup below.
        } catch (Exception e) {
          log.warn("Cycle name lookup failed for '{}': {}", candidateName, e.getMessage());
        }
      }

      // No name match found; fall back to numeric ID (e.g. "go to cycle 5" really
      // meaning the row with id=5).
      if (numericId != null) {
        request.setContextId(numericId);
        if (request.getCycleId() == null) {
          request.setCycleId(numericId);
        }
        try {
          cycleRepository.findById(numericId)
              .ifPresent(c -> request.setContextName(c.getName()));
        } catch (Exception e) {
          log.debug("Could not resolve cycle name for ID {}: {}", numericId, e.getMessage());
        }
        log.info("Resolved cycle by numeric ID: contextId={}, contextName='{}'",
            numericId, request.getContextName());
        return;
      }

      // Last resort: try a free-form name extraction (e.g. "the Build Season cycle")
      String freeName = extractEntityNameFromQuestion(question, contextType);
      if (freeName != null) {
        try {
          List<Cycle> freeMatches = cycleRepository.findByNameContainingIgnoreCase(freeName);
          final String finalFreeName = freeName;
          List<Cycle> exactFreeMatches = freeMatches.stream()
              .filter(c -> c.getName() != null && c.getName().equalsIgnoreCase(finalFreeName))
              .collect(Collectors.toList());

          if (exactFreeMatches.size() == 1) {
            Cycle cycle = exactFreeMatches.get(0);
            request.setContextId(cycle.getId());
            if (request.getCycleId() == null) {
              request.setCycleId(cycle.getId());
            }
            request.setContextName(cycle.getName());
            log.info("Resolved cycle by exact free-form name '{}' → ID={}", freeName, cycle.getId());
          } else if (freeMatches.size() > 1) {
            request.setContextName(freeName);
            log.info("Multiple cycles match free-form name '{}' ({})", freeName, freeMatches.size());
          } else {
            request.setContextName(freeName);
            log.debug("No cycle found for free-form name '{}', relying on semantic search", freeName);
          }
        } catch (Exception e) {
          log.warn("Cycle free-form name lookup failed for '{}': {}", freeName, e.getMessage());
        }
      }
      return;
    }

    // Non-cycle entities: numeric ID extraction only.
    Long extractedId = extractEntityIdFromQuestion(question, contextType);
    if (extractedId != null) {
      request.setContextId(extractedId);
      if ("team".equalsIgnoreCase(contextType) && request.getTeamId() == null) {
        request.setTeamId(extractedId);
      }
      log.info("Resolved entity from question: contextType={}, contextId={}",
          contextType, extractedId);
    }
  }

  /**
   * Extract entity name from question text (non-numeric references).
   * Examples: "the Build Season cycle" → "Build Season",
   * "pitches in Q1 Sprint" → "Q1 Sprint"
   */
  private String extractEntityNameFromQuestion(String question, String contextType) {
    if (question == null || contextType == null) {
      return null;
    }

    // Quote contextType so user-supplied values cannot inject regex metacharacters
    // (ReDoS / regex-injection).
    String quotedType = java.util.regex.Pattern.quote(contextType.toLowerCase());

    // Pattern 1: "the XYZ cycle", "the XYZ pitch"
    java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(
        "(?:the|this)\\s+(.+?)\\s+" + quotedType, java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher m1 = p1.matcher(question);
    if (m1.find()) {
      String name = m1.group(1).trim();
      // Skip if it's just a number
      if (!name.matches("\\d+")) {
        return name;
      }
    }

    // Pattern 2: "in/for/about XYZ cycle"
    java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
        "(?:in|for|about|from)\\s+(?:the\\s+)?(.+?)\\s+" + quotedType, java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher m2 = p2.matcher(question);
    if (m2.find()) {
      String name = m2.group(1).trim();
      if (!name.matches("\\d+")) {
        return name;
      }
    }

    // Pattern 3: "cycle XYZ" where XYZ is not a number (e.g., "cycle Alpha").
    // The terminal anchor uses a literal '?' rather than \s*\? to avoid polynomial
    // backtracking.
    java.util.regex.Pattern p3 = java.util.regex.Pattern.compile(
        quotedType + "\\s+(?!\\d)(\\S.+?)(?:\\?|$)", java.util.regex.Pattern.CASE_INSENSITIVE);
    java.util.regex.Matcher m3 = p3.matcher(question);
    if (m3.find()) {
      String name = m3.group(1).trim();
      // Remove trailing '?' with a plain check — avoids polynomial backtracking from
      // \s*\?\s*$.
      if (name.endsWith("?")) {
        name = name.substring(0, name.length() - 1).trim();
      }
      // Remove trailing auxiliary verb with plain suffix checks — avoids backtracking
      // from \s+(...verb...)$.
      for (String verb : new String[] { "has", "have", "is", "are", "was", "were", "do", "does", "did" }) {
        if (name.endsWith(" " + verb)) {
          name = name.substring(0, name.length() - verb.length()).trim();
          break;
        }
      }
      if (!name.isEmpty() && !name.matches("\\d+")) {
        return name;
      }
    }

    return null;
  }

  /**
   * Extract entity ID from question text based on context type. Examples: "cycle
   * 4" → 4, "pitch 15" → 15, "team 2" → 2
   */
  private Long extractEntityIdFromQuestion(String question, String contextType) {
    if (question == null || contextType == null) {
      return null;
    }

    String lowerQuestion = question.toLowerCase().trim();
    // Quote contextType to prevent regex injection from user-controlled values.
    String quotedContextType = java.util.regex.Pattern.quote(contextType.toLowerCase());

    // Pattern 1: "cycle 4", "pitch 15", "team 2", "meeting 3"
    java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile("\\b" + quotedContextType + "\\s+(\\d+)\\b");
    java.util.regex.Matcher matcher1 = pattern1.matcher(lowerQuestion);
    if (matcher1.find()) {
      try {
        return Long.parseLong(matcher1.group(1));
      } catch (NumberFormatException e) {
        log.warn("Failed to parse ID from: {}", matcher1.group(1));
      }
    }

    // Pattern 2: Just a number if the question is very short (e.g., "4", "15")
    if (lowerQuestion.matches("^\\d+$")) {
      try {
        return Long.parseLong(lowerQuestion);
      } catch (NumberFormatException e) {
        log.warn("Failed to parse ID from: {}", lowerQuestion);
      }
    }

    // Pattern 3: "in cycle 4", "for pitch 15", "about team 2"
    java.util.regex.Pattern pattern3 = java.util.regex.Pattern
        .compile("\\b(?:in|for|about|from)\\s+" + quotedContextType + "\\s+(\\d+)\\b");
    java.util.regex.Matcher matcher3 = pattern3.matcher(lowerQuestion);
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
   * Check if the question contains ambiguous contextual references without
   * providing context. Returns a clarification message if ambiguous, null
   * otherwise.
   */
  private String checkForAmbiguousContext(AskQuestionRequest request) {
    String question = request.getQuestion().toLowerCase();

    // Only check for ambiguity if contextType is set but contextId is null
    // This means the request came from a context-aware page but without specific
    // context
    if (request.getContextType() == null || request.getContextId() != null) {
      return null; // No ambiguity if no context type or context ID is already set
    }

    // Check for "this cycle" or "the cycle" - truly ambiguous references
    // But NOT "cycle 5", "cycle planning", etc - those are explicit mentions
    if (request.getContextType().equalsIgnoreCase("cycle")) {
      if (question.contains("this cycle") || question.contains("the cycle")) {
        return "I noticed you're asking about a cycle, but I need to know which cycle you're referring to. "
            + "Could you please specify which cycle you'd like to know about? For example: "
            + "\"What pitches are in cycle 5?\" or visit a specific cycle page to ask contextual questions.";
      }
    }

    // Check for "this pitch" or "the pitch" - truly ambiguous references
    if (request.getContextType().equalsIgnoreCase("pitch")) {
      if (question.contains("this pitch") || question.contains("the pitch")) {
        return "I noticed you're asking about a pitch, but I need to know which pitch you're referring to. "
            + "Could you please specify the pitch name or visit a specific pitch page to ask contextual questions.";
      }
    }

    // Check for "this team" or "the team" - truly ambiguous references
    if (request.getContextType().equalsIgnoreCase("team")) {
      if (question.contains("this team") || question.contains("the team")) {
        return "I noticed you're asking about a team, but I need to know which team you're referring to. "
            + "Could you please specify the team name or visit a specific team page to ask contextual questions.";
      }
    }

    // Check for "this meeting" or "the meeting" - truly ambiguous references
    if (request.getContextType().equalsIgnoreCase("meeting")) {
      if (question.contains("this meeting") || question.contains("the meeting")) {
        return "I noticed you're asking about a meeting, but I need to know which meeting you're referring to. "
            + "Could you please specify the meeting or visit a specific meeting page to ask contextual questions?";
      }
    }

    return null; // No ambiguity detected
  }

  /** Generate clarification suggestions when ambiguous context is detected. */
  private List<String> generateClarificationSuggestions(AskQuestionRequest request) {
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
}
