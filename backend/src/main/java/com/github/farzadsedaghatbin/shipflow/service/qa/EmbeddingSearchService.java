package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.config.QAConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.AskQuestionRequest;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.service.MessageService;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Service for vector search orchestration.
 * Handles embedding generation, vector store search, context filtering, and fallback strategies.
 */
@Service
@Slf4j
public class EmbeddingSearchService {

  @Value("${app.qa.retrieval.top-k:5}")
  private int topK;

  private final EmbeddingModel embeddingModel;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final KnowledgeItemRepository knowledgeItemRepository;
  private final DocumentReranker documentReranker;
  private final ContextWindowManager contextWindowManager;
  private final QuestionProcessingService questionProcessingService;
  private final QAConfig qaConfig;
  private final MessageService messageService;

  @Autowired
  public EmbeddingSearchService(
      @Autowired(required = false) EmbeddingModel embeddingModel,
      @Autowired(required = false) EmbeddingStore<TextSegment> embeddingStore,
      KnowledgeItemRepository knowledgeItemRepository,
      @Autowired(required = false) DocumentReranker documentReranker,
      @Autowired(required = false) ContextWindowManager contextWindowManager,
      QuestionProcessingService questionProcessingService,
      @Autowired(required = false) QAConfig qaConfig,
      MessageService messageService) {
    this.embeddingModel = embeddingModel;
    this.embeddingStore = embeddingStore;
    this.knowledgeItemRepository = knowledgeItemRepository;
    this.documentReranker = documentReranker;
    this.contextWindowManager = contextWindowManager;
    this.questionProcessingService = questionProcessingService;
    this.qaConfig = qaConfig;
    this.messageService = messageService;
  }

  /**
   * Perform vector search for the given question.
   */
  public SearchResult search(String question, AskQuestionRequest request) {
    // Embed the question with retry logic
    Embedding questionEmbedding;
    try {
      questionEmbedding = embeddingModel.embed(question).content();
    } catch (Exception e) {
      log.error("Embedding service failed: {}", e.getMessage(), e);
      throw new RuntimeException(messageService.getMessage("error.qa.embedding.unavailable"), e);
    }

    // Retrieve relevant knowledge chunks
    int retrieveK = qaConfig != null ? qaConfig.getTopK() : topK;
    double minScore = 0.70;
    List<EmbeddingMatch<TextSegment>> matches;

    try {
      EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
          .queryEmbedding(questionEmbedding)
          .maxResults(retrieveK * 2)
          .minScore(minScore)
          .build();

      EmbeddingSearchResult<TextSegment> searchResult = embeddingStore.search(searchRequest);
      matches = searchResult.matches();
      log.debug("Vector store returned {} matches", matches.size());
    } catch (Exception e) {
      log.error("Vector store search failed, attempting fallback: {}", e.getMessage());
      matches = attemptFallbackSearch(request);
      if (matches == null || matches.isEmpty()) {
        throw new RuntimeException(messageService.getMessage("error.qa.vector.store.unavailable"), e);
      }
    }

    // Filter by context
    if (request.getCycleId() != null || request.getTeamId() != null || request.getContextId() != null) {
      matches = filterMatchesByContext(matches, request);
    }

    // Filter by minimum relevance
    matches = matches.stream()
        .filter(match -> match.score() >= minScore)
        .collect(Collectors.toList());

    // Apply recency boost
    matches = applyRecencyBoost(matches);

    // Re-rank for better ordering
    if (documentReranker != null && matches.size() > retrieveK) {
      matches = documentReranker.rerank(question, matches, retrieveK);
    } else {
      matches = matches.stream().limit(retrieveK).collect(Collectors.toList());
    }

    log.debug("Retrieved {} documents with avg score: {}", matches.size(),
        matches.stream().mapToDouble(EmbeddingMatch::score).average().orElse(0));

    // Build context with token management
    String context;
    boolean contextTruncated = false;
    if (contextWindowManager != null) {
      ContextWindowManager.ContextResult contextResult = 
          contextWindowManager.buildManagedContext(matches, question, 4000);
      context = contextResult.getContext();
      contextTruncated = contextResult.isWasTruncated();
    } else {
      context = buildContext(matches);
    }

    return SearchResult.builder()
        .matches(matches)
        .context(context)
        .contextTruncated(contextTruncated)
        .build();
  }

  /**
   * Search database by keywords when semantic search fails.
   */
  public List<EmbeddingMatch<TextSegment>> searchDatabaseByKeywords(
      String question, String contextType, Long contextId) {
    try {
      if (question == null || question.trim().isEmpty()) {
        return Collections.emptyList();
      }

      List<String> searchTerms = questionProcessingService.extractSearchTerms(question);
      log.info("Extracted search terms from question: {}", searchTerms);

      List<KnowledgeItem> allItems = knowledgeItemRepository.findAll();
      long embeddedCount = allItems.stream().filter(KnowledgeItem::getIsEmbedded).count();
      log.info("Database keyword search: total items={}, embedded items={}", 
          allItems.size(), embeddedCount);

      // Log entity type distribution
      Map<String, Long> entityTypeCounts = allItems.stream()
          .filter(KnowledgeItem::getIsEmbedded)
          .collect(Collectors.groupingBy(item -> item.getEntityType().name(), Collectors.counting()));
      log.info("Entity type distribution: {}", entityTypeCounts);

      boolean contextTypeHasItems = contextType == null || allItems.stream()
          .anyMatch(item -> item.getIsEmbedded() && 
              item.getEntityType().name().equalsIgnoreCase(contextType));

      List<KnowledgeItem> items;
      if (contextType != null && contextId != null) {
        items = searchWithContext(allItems, searchTerms, contextType, contextId);
      } else if (contextType != null && contextTypeHasItems) {
        items = searchByEntityType(allItems, searchTerms, contextType);
      } else {
        items = searchAllItems(allItems, searchTerms);
      }

      log.info("Database keyword search found {} items", items.size());

      return items.stream()
          .map(item -> createMatchFromItem(item))
          .collect(Collectors.toList());

    } catch (Exception e) {
      log.error("Database keyword search failed: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Filter matches by context (cycle, team, entity).
   */
  public List<EmbeddingMatch<TextSegment>> filterMatchesByContext(
      List<EmbeddingMatch<TextSegment>> matches, AskQuestionRequest request) {

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

      // Filter by specific entity context
      if (request.getContextType() != null && request.getContextId() != null) {
        String entityType = segment.metadata().getString("entityType");
        String entityId = segment.metadata().getString("entityId");
        if (!request.getContextType().equalsIgnoreCase(entityType)
            || !request.getContextId().toString().equals(entityId)) {
          return true; // Allow related items
        }
      }

      return true;
    }).collect(Collectors.toList());
  }

  /**
   * Apply recency boost to search results.
   */
  public List<EmbeddingMatch<TextSegment>> applyRecencyBoost(
      List<EmbeddingMatch<TextSegment>> matches) {
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
                  boostFactor = 1.15;
                } else if (updatedAt.isAfter(thirtyDaysAgo)) {
                  boostFactor = 1.10;
                } else if (updatedAt.isAfter(ninetyDaysAgo)) {
                  boostFactor = 1.05;
                }
              } catch (Exception e) {
                log.debug("Could not parse updatedAt for recency boost: {}", updatedAtStr);
              }
            }
          }

          double boostedScore = Math.min(1.0, match.score() * boostFactor);
          return new EmbeddingMatch<>(boostedScore, match.embeddingId(), match.embedding(), match.embedded());
        })
        .sorted((a, b) -> Double.compare(b.score(), a.score()))
        .collect(Collectors.toList());
  }

  /**
   * Build context string from matches.
   */
  public String buildContext(List<EmbeddingMatch<TextSegment>> matches) {
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

  /**
   * Check if search services are available.
   */
  public boolean isSearchAvailable() {
    return embeddingModel != null && embeddingStore != null;
  }

  // ===== Private helper methods =====

  private List<EmbeddingMatch<TextSegment>> attemptFallbackSearch(AskQuestionRequest request) {
    try {
      log.warn("Fallback search not yet implemented, returning empty results");
      return Collections.emptyList();
    } catch (Exception e) {
      log.error("Fallback search also failed: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  private List<KnowledgeItem> searchWithContext(
      List<KnowledgeItem> allItems, List<String> searchTerms, 
      String contextType, Long contextId) {
    return allItems.stream()
        .filter(KnowledgeItem::getIsEmbedded)
        .filter(item -> {
          String content = (item.getContent() + " " + 
              (item.getTitle() != null ? item.getTitle() : "")).toLowerCase();
          boolean matchesSearch = searchTerms.stream()
              .anyMatch(term -> content.contains(term));
          boolean matchesContext = contextType.equalsIgnoreCase(item.getEntityType().name())
              && contextId.equals(item.getEntityId());
          return matchesSearch || matchesContext;
        })
        .limit(5)
        .collect(Collectors.toList());
  }

  private List<KnowledgeItem> searchByEntityType(
      List<KnowledgeItem> allItems, List<String> searchTerms, String contextType) {
    return allItems.stream()
        .filter(KnowledgeItem::getIsEmbedded)
        .filter(item -> {
          String content = (item.getContent() + " " + 
              (item.getTitle() != null ? item.getTitle() : "")).toLowerCase();
          return searchTerms.stream().anyMatch(term -> content.contains(term))
              && contextType.equalsIgnoreCase(item.getEntityType().name());
        })
        .limit(5)
        .collect(Collectors.toList());
  }

  private List<KnowledgeItem> searchAllItems(
      List<KnowledgeItem> allItems, List<String> searchTerms) {
    return allItems.stream()
        .filter(KnowledgeItem::getIsEmbedded)
        .filter(item -> {
          String content = (item.getContent() + " " + 
              (item.getTitle() != null ? item.getTitle() : "")).toLowerCase();
          return searchTerms.stream().anyMatch(term -> content.contains(term));
        })
        .limit(5)
        .collect(Collectors.toList());
  }

  private EmbeddingMatch<TextSegment> createMatchFromItem(KnowledgeItem item) {
    TextSegment segment = TextSegment.from(
        item.getContent(),
        new Metadata()
            .put("id", item.getId().toString())
            .put("entityType", item.getEntityType().name())
            .put("entityId", item.getEntityId().toString())
            .put("title", item.getTitle() != null ? item.getTitle() : "")
    );
    return new EmbeddingMatch<>(0.75, item.getEmbeddingId(), null, segment);
  }

  /**
   * Result of embedding search.
   */
  @Getter
  @Builder
  @AllArgsConstructor
  public static class SearchResult {
    private final List<EmbeddingMatch<TextSegment>> matches;
    private final String context;
    private final boolean contextTruncated;
  }
}
