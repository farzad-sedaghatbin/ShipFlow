package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.AIConfig;
import com.github.farzadsedaghatbin.shipflow.config.QAConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.*;
import com.github.farzadsedaghatbin.shipflow.entity.QAInteraction;
import com.github.farzadsedaghatbin.shipflow.entity.enums.QAFeedbackType;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service for handling Q&A interactions using RAG (Retrieval-Augmented Generation).
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

    @Autowired
    public QAService(
            KnowledgeItemRepository knowledgeItemRepository,
            QAInteractionRepository qaInteractionRepository,
            @Autowired(required = false) EmbeddingModel embeddingModel,
            @Autowired(required = false) EmbeddingStore<TextSegment> embeddingStore,
            @Autowired(required = false) ChatLanguageModel chatLanguageModel,
            @Autowired(required = false) KnowledgeIngestionService knowledgeIngestionService,
            @Autowired(required = false) QAConfig qaConfig,
            @Autowired(required = false) AIConfig aiConfig,
            AICacheService cacheService,
            @Autowired(required = false) RAGEvaluator ragEvaluator,
            @Autowired(required = false) DocumentReranker documentReranker,
            @Autowired(required = false) ContextWindowManager contextWindowManager,
            @Autowired(required = false) ConversationManager conversationManager,
            @Autowired(required = false) SecurityDocumentFilter securityFilter,
            @Autowired(required = false) QueryDecomposer queryDecomposer,
            @Autowired(required = false) FeedbackLearningService feedbackLearningService,
            @Autowired(required = false) LLMCacheService llmCacheService,
            @Autowired(required = false) PromptCompressor promptCompressor,
            @Autowired(required = false) ContentGuardrails contentGuardrails,
            MessageService messageService) {
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
    }

    /**
     * Ask a question and get an AI-generated answer based on retrieved knowledge.
     */
    @Transactional
    public QAResponse askQuestion(AskQuestionRequest request, Long userId) {
        long startTime = System.currentTimeMillis();

        if (!isQAEnabled()) {
            return QAResponse.builder()
                    .question(request.getQuestion())
                    .aiEnabled(false)
                    .errorMessage("Q&A feature is not enabled")
                    .answeredAt(LocalDateTime.now())
                    .build();
        }

        // Check cache first for similar questions
        Optional<QAResponse> cachedResponse = cacheService.getCachedQAResponse(
                request.getQuestion(),
                request.getContextType(),
                request.getContextId(),
                request.getCycleId(),
                request.getTeamId());
        
        if (cachedResponse.isPresent()) {
            log.debug("Returning cached Q&A response for similar question");
            QAResponse cached = cachedResponse.get();
            // Return cached response with updated metadata
            return QAResponse.builder()
                    .question(request.getQuestion())
                    .answer(cached.getAnswer())
                    .sources(cached.getSources())
                    .confidenceScore(cached.getConfidenceScore())
                    .answeredAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .aiEnabled(cached.getAiEnabled())
                    .suggestedFollowUps(cached.getSuggestedFollowUps())
                    .cached(true)  // Mark as cached response
                    .build();
        }

        // Get or create conversation context early so it can be used for context inference
        ConversationContext conversation = null;
        if (conversationManager != null && request.getConversationId() != null) {
            try {
                conversation = conversationManager.getOrCreateConversation(
                    request.getConversationId(),
                    userId,
                    request.getContextType(),
                    request.getContextId()
                );
            } catch (Exception e) {
                log.warn("Failed to get/create conversation, continuing without history: {}", e.getMessage());
                // Continue without conversation memory - graceful degradation
            }
        }

        // Note: We don't automatically extract IDs from questions like "cycle 4"
        // because "cycle 4" could be part of a name (e.g., "Cycle 5 - Build Sprint")
        // Let semantic search handle finding entities by name naturally.
        // IDs should be provided explicitly via contextId parameter (e.g., from URL context)
        
        // Try to infer context from conversation history if not provided
        if (conversation != null && conversationManager != null && 
            request.getContextId() == null && request.getContextType() != null) {
            try {
                ConversationContext.ContextInfo previousContext = 
                    conversationManager.getMostRecentContext(conversation.getConversationId());
                if (previousContext != null && 
                    previousContext.getContextType().equalsIgnoreCase(request.getContextType())) {
                    request.setContextId(previousContext.getContextId());
                    log.info("Inferred contextId={} from conversation history", previousContext.getContextId());
                }
            } catch (Exception e) {
                log.debug("Could not infer context from conversation: {}", e.getMessage());
            }
        }
        
        // Check for ambiguous contextual references
        String ambiguityCheck = checkForAmbiguousContext(request);
        if (ambiguityCheck != null) {
            log.info("Detected ambiguous context in question: {}", request.getQuestion());
            return QAResponse.builder()
                    .question(request.getQuestion())
                    .answer(ambiguityCheck)
                    .confidenceScore(0)
                    .answeredAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .aiEnabled(true)
                    .suggestedFollowUps(generateClarificationSuggestions(request))
                    .cached(false)
                    .build();
        }
        
        String originalQuestion = request.getQuestion();

        try {
            // 0. Query decomposition for complex questions
            List<String> subQueries = List.of(request.getQuestion());
            boolean wasDecomposed = false;
            
            if (queryDecomposer != null && queryDecomposer.shouldDecompose(request.getQuestion())) {
                try {
                    QueryDecomposer.DecompositionResult decomposition = 
                            queryDecomposer.decomposeWithMetadata(request.getQuestion());
                    if (decomposition.wasDecomposed()) {
                        subQueries = decomposition.getSubQueries();
                        wasDecomposed = true;
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
                        .queryEmbedding(questionEmbedding)
                        .maxResults(retrieveK * 2) // Retrieve more, then filter
                        .minScore(minScore)
                        .build();

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
                            request.getQuestion(), 
                            request.getContextType(),
                            request.getContextId(),
                            request.getCycleId(),
                            request.getTeamId());
                    
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
                // matches = securityFilter.filterByUserAccess(matches, userId, userTeamIds, userProjectIds);
            }

            // 4. Filter by context
            if (request.getCycleId() != null || request.getTeamId() != null || 
                    request.getContextId() != null) {
                matches = filterMatchesByContext(matches, request);
            }
            
            // 5. Filter by minimum relevance
            matches = matches.stream()
                    .filter(match -> match.score() >= minScore)
                    .collect(Collectors.toList());
            
            // 6. Re-rank for better ordering
            if (documentReranker != null && matches.size() > retrieveK) {
                matches = documentReranker.rerank(request.getQuestion(), matches, retrieveK);
            } else {
                matches = matches.stream().limit(retrieveK).collect(Collectors.toList());
            }
            
            // Log retrieval metrics
            log.debug("Retrieved {} documents with avg score: {}", 
                    matches.size(), 
                    matches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0));

            // 7. Build context with token management
            String context;
            boolean contextTruncated = false;
            if (contextWindowManager != null) {
                ContextWindowManager.ContextResult contextResult = contextWindowManager.buildManagedContext(
                    matches, request.getQuestion(), 4000
                );
                context = contextResult.getContext();
                contextTruncated = contextResult.isWasTruncated();
            } else {
                context = buildContext(matches);
            }
            
            List<QAResponse.SourceCitation> citations = buildCitations(matches);
            
            // 8. Add conversation history if available
            String conversationHistory = "";
            if (conversation != null && conversationManager != null) {
                conversationHistory = conversationManager.buildConversationHistory(
                    conversation.getConversationId(), 3
                );
            }

            // 9. Generate answer using LLM with rate limit handling, caching, compression, and guardrails
            String answer;
            int confidenceScore;
            RAGEvaluationMetrics ragMetrics = null;

            if (chatLanguageModel != null && !context.isEmpty()) {
                try {
                    // Build and compress prompt
                    String prompt = buildPrompt(request.getQuestion(), context, conversationHistory);
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
                        double patternSuccessRate = feedbackLearningService.getPatternSuccessRate(request.getQuestion());
                        if (patternSuccessRate < 0.5) {
                            log.warn("Query pattern has low success rate: {}", patternSuccessRate);
                            confidenceScore = (int) (confidenceScore * 0.9); // Reduce confidence
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
                    if (conversation != null && conversationManager != null) {
                        try {
                            conversationManager.addTurn(
                                conversation.getConversationId(),
                                request.getQuestion(),
                                answer
                            );
                        } catch (Exception e) {
                            log.warn("Failed to save conversation turn: {}", e.getMessage());
                            // Continue - this is not critical
                        }
                    }
                    
                    // Evaluate RAG quality
                    if (ragEvaluator != null) {
                        try {
                            ragMetrics = ragEvaluator.evaluate(request.getQuestion(), answer, matches);
                            log.debug("RAG Metrics - Faithfulness: {}, Relevance: {}", 
                                    ragMetrics.getFaithfulness(), ragMetrics.getAnswerRelevance());
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
                log.info("No semantic matches found, attempting database keyword search for: {}", request.getQuestion());
                List<EmbeddingMatch<TextSegment>> databaseMatches = searchDatabaseByKeywords(
                    request.getQuestion(), 
                    request.getContextType(), 
                    request.getContextId()
                );
                
                if (!databaseMatches.isEmpty()) {
                    log.info("Found {} database matches", databaseMatches.size());
                    matches = databaseMatches;
                    context = buildContext(matches);
                    
                    if (chatLanguageModel != null && !context.isEmpty()) {
                        try {
                            // Build and compress prompt
                            String prompt = buildPrompt(request.getQuestion(), context, conversationHistory);
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
                    answer = "I couldn't find relevant information to answer your question. " +
                            "Try rephrasing or asking about something else related to your cycles, pitches, or meetings.";
                    confidenceScore = 0;
                }
            }

            // 6. Save the interaction
            QAInteraction interaction = QAInteraction.builder()
                    .question(originalQuestion) // Save original question
                    .answer(answer)
                    .contextType(request.getContextType())
                    .contextId(request.getContextId())
                    .cycleId(request.getCycleId())
                    .teamId(request.getTeamId())
                    .userId(userId)
                    .sourceKnowledgeIds(buildSourceIds(matches))
                    .confidenceScore(confidenceScore)
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();

            interaction = qaInteractionRepository.save(interaction);

            // 11. Build response
            QAResponse response = QAResponse.builder()
                    .interactionId(interaction.getId())
                    .question(originalQuestion) // Use original question in response
                    .answer(answer)
                    .sources(request.getIncludeSources() ? citations : null)
                    .confidenceScore(confidenceScore)
                    .answeredAt(LocalDateTime.now())
                    .processingTimeMs(interaction.getProcessingTimeMs())
                    .aiEnabled(chatLanguageModel != null)
                    .suggestedFollowUps(generateSuggestedFollowUps(request, matches))
                    .cached(false)
                    .conversationId(conversation != null ? conversation.getConversationId() : null)
                    .build();

            // 8. Cache the response for future similar questions
            cacheService.cacheQAResponse(
                    originalQuestion, // Cache with original question
                    request.getContextType(),
                    request.getContextId(),
                    request.getCycleId(),
                    request.getTeamId(),
                    response);

            return response;

        } catch (Exception e) {
            log.error("Error processing question: {}", e.getMessage(), e);
            
            return QAResponse.builder()
                    .question(request.getQuestion())
                    .aiEnabled(chatLanguageModel != null)
                    .errorMessage("Failed to process question: " + e.getMessage())
                    .answeredAt(LocalDateTime.now())
                    .processingTimeMs(System.currentTimeMillis() - startTime)
                    .build();
        }
    }

    /**
     * Submit feedback for a Q&A response.
     */
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
        if (request.getFeedbackType() == QAFeedbackType.ACCURATE || 
                request.getFeedbackType() == QAFeedbackType.CORRECTED) {
            if (knowledgeIngestionService != null) {
                knowledgeIngestionService.ingestValidatedQA(interaction.getId());
            }
        }

        log.info("Feedback submitted for interaction {}: {}", interaction.getId(), request.getFeedbackType());
    }    
    /**
     * Record simple helpful/unhelpful feedback for active learning.
     */
    @Transactional
    public void recordSimpleFeedback(Long interactionId, boolean helpful, String text) {
        if (feedbackLearningService != null) {
            feedbackLearningService.recordFeedback(interactionId, helpful, text);
            log.info("Active learning feedback recorded for interaction {}: helpful={}", interactionId, helpful);
        } else {
            log.warn("FeedbackLearningService not available, feedback not recorded");
        }
    }
    /**
     * Get the status of the Q&A feature.
     */
    public QAStatusDTO getStatus() {
        return QAStatusDTO.builder()
                .qaEnabled(isQAEnabled())
                .aiAvailable(chatLanguageModel != null)
                .vectorStoreType(qaConfig != null ? qaConfig.getVectorStoreType() : "none")
                .totalKnowledgeItems(knowledgeItemRepository.count())
                .embeddedKnowledgeItems(knowledgeItemRepository.countEmbedded())
                .totalInteractions(qaInteractionRepository.count())
                .validatedInteractions(qaInteractionRepository.countValidated(
                        List.of(QAFeedbackType.ACCURATE, QAFeedbackType.CORRECTED)))
                .embeddingModel("all-MiniLM-L6-v2")
                .llmModel(aiConfig != null ? aiConfig.getModelName() : "none")
                .build();
    }

    /**
     * Get recent interactions for a user.
     */
    public List<QAInteraction> getRecentInteractions(Long userId) {
        return qaInteractionRepository.findTop10ByUserIdOrderByCreatedAtDesc(userId);
    }

    // ===== Private helper methods =====

    private boolean isQAEnabled() {
        return qaEnabled && embeddingModel != null && embeddingStore != null;
    }

    private List<EmbeddingMatch<TextSegment>> filterMatchesByContext(
            List<EmbeddingMatch<TextSegment>> matches, 
            AskQuestionRequest request) {
        
        return matches.stream()
                .filter(match -> {
                    TextSegment segment = match.embedded();
                    if (segment == null || segment.metadata() == null) return true;
                    
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
                    
                    // Filter by specific entity context
                    if (request.getContextType() != null && request.getContextId() != null) {
                        String entityType = segment.metadata().getString("entityType");
                        String entityId = segment.metadata().getString("entityId");
                        if (!request.getContextType().equalsIgnoreCase(entityType) ||
                                !request.getContextId().toString().equals(entityId)) {
                            // Allow if it's related (same cycle/team) even if not exact match
                            return true;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
    }

    private String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
        if (matches.isEmpty()) return "";

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < matches.size(); i++) {
            EmbeddingMatch<TextSegment> match = matches.get(i);
            TextSegment segment = match.embedded();
            
            String title = segment.metadata() != null ? 
                    segment.metadata().getString("title") : "Unknown";
            
            context.append("[Source ").append(i + 1).append(": ").append(title).append("]\n");
            context.append(segment.text()).append("\n\n");
        }
        
        return context.toString();
    }

    private List<QAResponse.SourceCitation> buildCitations(List<EmbeddingMatch<TextSegment>> matches) {
        List<QAResponse.SourceCitation> citations = new ArrayList<>();
        
        for (EmbeddingMatch<TextSegment> match : matches) {
            TextSegment segment = match.embedded();
            if (segment == null) continue;

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
                    } catch (NumberFormatException ignored) {}
                }
                entityType = metadata.getString("entityType");
                String eid = metadata.getString("entityId");
                if (eid != null) {
                    try {
                        entityId = Long.parseLong(eid);
                    } catch (NumberFormatException ignored) {}
                }
                title = metadata.getString("title");
            }

            // Create snippet (first 200 chars)
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

    private String buildSourceIds(List<EmbeddingMatch<TextSegment>> matches) {
        return matches.stream()
                .map(match -> {
                    if (match.embedded() != null && match.embedded().metadata() != null) {
                        return match.embedded().metadata().getString("knowledgeItemId");
                    }
                    return null;
                })
                .filter(id -> id != null)
                .collect(Collectors.joining(","));
    }

    private String buildPrompt(String question, String context, String conversationHistory) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("""
            You are a helpful assistant for the ShipFlow application, which helps teams manage their work using the Shape Up methodology.
            
            Use the following context to answer the user's question. If the context doesn't contain enough information to fully answer the question, say so and provide what information you can.
            
            Be concise but thorough. If referencing specific sources, mention them naturally in your response.
            """);
        
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
        if (matches.isEmpty()) return 0;
        
        // Calculate average similarity score
        double avgScore = matches.stream()
                .mapToDouble(EmbeddingMatch::score)
                .average()
                .orElse(0.0);
        
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
     * Attempt fallback search when vector store fails.
     * This could use a backup vector store or basic text matching.
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
    
    /**
     * Detect if the exception is a rate limit error from the LLM provider.
     */
    private boolean isRateLimitError(Exception e) {
        String message = e.getMessage();
        if (message == null) return false;
        
        // Check for common rate limit indicators
        return message.toLowerCase().contains("rate limit") ||
               message.toLowerCase().contains("429") ||
               message.toLowerCase().contains("too many requests") ||
               message.toLowerCase().contains("quota exceeded");
    }
    
    /**
     * Search database for knowledge items using keyword matching when semantic search fails.
     * This provides a fallback for short queries like "cycle 4" that don't embed well.
     */
    private List<EmbeddingMatch<TextSegment>> searchDatabaseByKeywords(
            String question, String contextType, Long contextId) {
        try {
            if (question == null || question.trim().isEmpty()) {
                return Collections.emptyList();
            }
            
            // Extract meaningful search terms from the question
            List<String> searchTerms = extractSearchTerms(question);
            log.info("Extracted search terms from question: {}", searchTerms);
            
            List<com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem> items;
            
            // Get total knowledge items for debugging
            List<com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem> allItems = knowledgeItemRepository.findAll();
            long embeddedCount = allItems.stream().filter(item -> item.getIsEmbedded()).count();
            log.info("Database keyword search: total items={}, embedded items={}, search terms={}, contextType={}, contextId={}", 
                     allItems.size(), embeddedCount, searchTerms, contextType, contextId);
            
            // Log entity type distribution
            Map<String, Long> entityTypeCounts = allItems.stream()
                .filter(item -> item.getIsEmbedded())
                .collect(Collectors.groupingBy(item -> item.getEntityType().name(), Collectors.counting()));
            log.info("Entity type distribution: {}", entityTypeCounts);
            
            // Log sample items matching contextType if specified
            if (contextType != null) {
                log.info("Looking for items with contextType={}", contextType);
                long matchingTypeCount = allItems.stream()
                    .filter(item -> item.getIsEmbedded() && item.getEntityType().name().equalsIgnoreCase(contextType))
                    .count();
                log.info("Found {} items with entityType={}", matchingTypeCount, contextType);
                
                if (matchingTypeCount > 0) {
                    allItems.stream()
                        .filter(item -> item.getIsEmbedded() && item.getEntityType().name().equalsIgnoreCase(contextType))
                        .limit(3)
                        .forEach(item -> {
                            String contentPreview = item.getContent() != null && item.getContent().length() > 100 
                                ? item.getContent().substring(0, 100) + "..." 
                                : item.getContent();
                            log.info("Sample {} item: entityId={}, title='{}', content preview='{}'", 
                                     item.getEntityType().name(), item.getEntityId(), item.getTitle(), contentPreview);
                        });
                } else {
                    log.info("No items found with entityType={}, will search across all entity types", contextType);
                }
            }
            
            // Search based on context
            // Check if the contextType actually has items in the database
            boolean contextTypeHasItems = contextType == null || 
                allItems.stream().anyMatch(item -> item.getIsEmbedded() && 
                                                   item.getEntityType().name().equalsIgnoreCase(contextType));
            
            if (contextType != null && contextId != null) {
                // Search within specific context
                items = knowledgeItemRepository.findAll().stream()
                    .filter(item -> item.getIsEmbedded())
                    .filter(item -> {
                        String content = (item.getContent() + " " + (item.getTitle() != null ? item.getTitle() : "")).toLowerCase();
                        boolean matchesSearch = searchTerms.stream().anyMatch(term -> content.contains(term));
                        boolean matchesContext = contextType.equalsIgnoreCase(item.getEntityType().name()) && 
                                                contextId.equals(item.getEntityId());
                        return matchesSearch || matchesContext;
                    })
                    .limit(5)
                    .collect(Collectors.toList());
            } else if (contextType != null && contextTypeHasItems) {
                // Search within entity type (only if that type exists)
                items = knowledgeItemRepository.findAll().stream()
                    .filter(item -> item.getIsEmbedded())
                    .filter(item -> {
                        String content = (item.getContent() + " " + (item.getTitle() != null ? item.getTitle() : "")).toLowerCase();
                        return searchTerms.stream().anyMatch(term -> content.contains(term)) &&
                               contextType.equalsIgnoreCase(item.getEntityType().name());
                    })
                    .limit(5)
                    .collect(Collectors.toList());
            } else {
                // General keyword search
                items = knowledgeItemRepository.findAll().stream()
                    .filter(item -> item.getIsEmbedded())
                    .filter(item -> {
                        String content = (item.getContent() + " " + (item.getTitle() != null ? item.getTitle() : "")).toLowerCase();
                        return searchTerms.stream().anyMatch(term -> content.contains(term));
                    })
                    .limit(5)
                    .collect(Collectors.toList());
            }
            
            log.info("Database keyword search found {} items matching any of: {}", items.size(), searchTerms);
            if (!items.isEmpty()) {
                log.debug("Sample matched item: entityType={}, entityId={}, title={}", 
                         items.get(0).getEntityType(), items.get(0).getEntityId(), items.get(0).getTitle());
            }
            
            // Convert to EmbeddingMatch format for compatibility
            return items.stream()
                .map(item -> {
                    TextSegment segment = TextSegment.from(
                        item.getContent(),
                        new dev.langchain4j.data.document.Metadata()
                            .put("id", item.getId().toString())
                            .put("entityType", item.getEntityType().name())
                            .put("entityId", item.getEntityId().toString())
                            .put("title", item.getTitle() != null ? item.getTitle() : "")
                    );
                    return new EmbeddingMatch<>(
                        0.75, // Fixed relevance score for keyword matches
                        item.getEmbeddingId(),
                        null, // No embedding for keyword match
                        segment
                    );
                })
                .collect(Collectors.toList());
                
        } catch (Exception e) {
            log.error("Database keyword search failed: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Extract meaningful search terms from a question.
     * Examples: 
     * - "Are there any at-risk pitches in cycle 5?" → ["cycle 5", "at-risk", "at_risk", "risk"]
     * - "cycle 4" → ["cycle 4", "4"]
     * - "What is the status of the API pitch?" → ["api", "status", "pitch"]
     */
    private List<String> extractSearchTerms(String question) {
        if (question == null || question.trim().isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> terms = new ArrayList<>();
        String lowerQuestion = question.toLowerCase().trim();
        
        // Extract entity + number patterns (cycle 5, pitch 4, etc.)
        java.util.regex.Pattern entityPattern = java.util.regex.Pattern.compile("(cycle|pitch|team|meeting)\\s+\\d+");
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
        if (lowerQuestion.contains("in progress") || lowerQuestion.contains("in-progress") || lowerQuestion.contains("progress")) {
            terms.add("in_progress");
            terms.add("in progress");
            terms.add("progress");
        }
        
        // Other status terms
        if (lowerQuestion.contains("started") || lowerQuestion.contains("start")) {
            terms.add("started");
            terms.add("start");
        }
        
        if (lowerQuestion.contains("completed") || lowerQuestion.contains("complete") || lowerQuestion.contains("done")) {
            terms.add("completed");
            terms.add("complete");
            terms.add("done");
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
        String cleaned = lowerQuestion.replaceAll("\\b(what|where|when|who|why|how|is|are|the|a|an|in|on|at|for|to|of|about|tell|me|show|any|there)\\b", " ")
            .replaceAll("\\s+", " ")
            .trim();
        
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
        return terms.stream()
            .filter(t -> t != null && !t.trim().isEmpty())
            .distinct()
            .collect(Collectors.toList());
    }
    
    /**
     * Check if a question is likely asking for an entity by ID only,
     * vs. being part of a name (e.g., "cycle 4" vs "Cycle 4 Planning").
     */
    private boolean isLikelyIdOnlyQuery(String question) {
        if (question == null) {
            return false;
        }
        
        String trimmed = question.trim();
        String lowerQuestion = trimmed.toLowerCase();
        
        // Just a number: "4"
        if (lowerQuestion.matches("^\\d+$")) {
            return true;
        }
        
        // Entity type + number with nothing else: "cycle 4", "pitch 15"
        if (lowerQuestion.matches("^(cycle|pitch|team|meeting)\\s+\\d+$")) {
            return true;
        }
        
        // Has additional text that looks like a name/title
        if (lowerQuestion.matches(".*(planning|build|q\\d|phase|sprint|\\w{5,}).*")) {
            return false; // Likely contains a name
        }
        
        // Short queries (1-2 words) are likely ID queries
        return trimmed.split("\\s+").length <= 2;
    }
    
    /**
     * Extract entity ID from question text based on context type.
     * Examples: "cycle 4" → 4, "pitch 15" → 15, "team 2" → 2
     */
    private Long extractEntityIdFromQuestion(String question, String contextType) {
        if (question == null || contextType == null) {
            return null;
        }
        
        String lowerQuestion = question.toLowerCase().trim();
        String lowerContextType = contextType.toLowerCase();
        
        // Pattern 1: "cycle 4", "pitch 15", "team 2", "meeting 3"
        java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile(
            "\\b" + lowerContextType + "\\s+(\\d+)\\b"
        );
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
        java.util.regex.Pattern pattern3 = java.util.regex.Pattern.compile(
            "\\b(?:in|for|about|from)\\s+" + lowerContextType + "\\s+(\\d+)\\b"
        );
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
     * Expand vague questions into more searchable queries.
     * Examples: "cycle 4" → "What pitches are in cycle 4?"
     */
    private String expandVagueQuestion(String question, String contextType, Long contextId) {
        if (question == null || question.trim().isEmpty()) {
            return question;
        }
        
        String trimmed = question.trim();
        String lowerQuestion = trimmed.toLowerCase();
        
        // Check if question is very short (just entity name/number or 1-2 words)
        String[] words = trimmed.split("\\s+");
        boolean isVague = words.length <= 2 || lowerQuestion.matches("^(\\d+|" + 
            (contextType != null ? contextType.toLowerCase() + "\\s*\\d+" : "\\d+") + ")$");
        
        if (!isVague) {
            return question; // Question is detailed enough
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
        } else if (contextType != null) {
            // Has context type but no ID
            return question; // Already handled by ambiguity check
        }
        
        // No context - return as is
        return question;
    }
    
    /**
     * Check if the question contains ambiguous contextual references without providing context.
     * Returns a clarification message if ambiguous, null otherwise.
     */
    private String checkForAmbiguousContext(AskQuestionRequest request) {
        String question = request.getQuestion().toLowerCase();
        
        // Only check for ambiguity if contextType is set but contextId is null
        // This means the request came from a context-aware page but without specific context
        if (request.getContextType() == null || request.getContextId() != null) {
            return null; // No ambiguity if no context type or context ID is already set
        }
        
        // Check for "this cycle" or "the cycle" - truly ambiguous references
        // But NOT "cycle 5", "cycle planning", etc - those are explicit mentions
        if (request.getContextType().equalsIgnoreCase("cycle")) {
            if (question.contains("this cycle") || question.contains("the cycle")) {
                return "I noticed you're asking about a cycle, but I need to know which cycle you're referring to. " +
                       "Could you please specify which cycle you'd like to know about? For example: " +
                       "\"What pitches are in cycle 5?\" or visit a specific cycle page to ask contextual questions.";
            }
        }
        
        // Check for "this pitch" or "the pitch" - truly ambiguous references
        if (request.getContextType().equalsIgnoreCase("pitch")) {
            if (question.contains("this pitch") || question.contains("the pitch")) {
                return "I noticed you're asking about a pitch, but I need to know which pitch you're referring to. " +
                       "Could you please specify the pitch name or visit a specific pitch page to ask contextual questions.";
            }
        }
        
        // Check for "this team" or "the team" - truly ambiguous references
        if (request.getContextType().equalsIgnoreCase("team")) {
            if (question.contains("this team") || question.contains("the team")) {
                return "I noticed you're asking about a team, but I need to know which team you're referring to. " +
                       "Could you please specify the team name or visit a specific team page to ask contextual questions.";
            }
        }
        
        // Check for "this meeting" or "the meeting" - truly ambiguous references
        if (request.getContextType().equalsIgnoreCase("meeting")) {
            if (question.contains("this meeting") || question.contains("the meeting")) {
                return "I noticed you're asking about a meeting, but I need to know which meeting you're referring to. " +
                       "Could you please specify the meeting or visit a specific meeting page to ask contextual questions?";
            }
        }
        
        return null; // No ambiguity detected
    }
    
    /**
     * Generate clarification suggestions when ambiguous context is detected.
     */
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