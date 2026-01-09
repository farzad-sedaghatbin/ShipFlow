package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.entity.QAInteraction;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Active learning service that improves RAG quality based on user feedback.
 * 
 * Tracks:
 * - Positive/negative feedback on answers
 * - Query patterns that work well/poorly
 * - Source relevance scores
 * - Re-ranking weight adjustments
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeedbackLearningService {
    
    private final QAInteractionRepository qaInteractionRepository;
    private final AICacheConfig cacheConfig;
    
    // Storage for feedback aggregation - uses Redis when configured, in-memory otherwise
    private final Map<String, FeedbackStats> queryPatternStats = new ConcurrentHashMap<>();
    private final Map<String, SourceRelevanceStats> sourceStats = new ConcurrentHashMap<>();
    
    // Redis client (lazy initialized if provider=redis)
    private Object redisClient; // Would be Jedis or Lettuce in production
    
    @PostConstruct
    public void init() {
        if (cacheConfig.isRedisProvider()) {
            initializeRedis();
            log.info("FeedbackLearningService initialized with Redis provider ({}:{})", 
                    cacheConfig.getRedis().getHost(), cacheConfig.getRedis().getPort());
        } else {
            log.info("FeedbackLearningService initialized with in-memory provider");
        }
    }
    
    /**
     * Initialize Redis connection for distributed feedback storage.
     * In production, this would use Spring Data Redis or Jedis/Lettuce client.
     */
    private void initializeRedis() {
        try {
            AICacheConfig.RedisConfig redis = cacheConfig.getRedis();
            log.info("Initializing Redis for feedback learning at {}:{}", redis.getHost(), redis.getPort());
            // In production implementation:
            // RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
            // redisTemplate.setConnectionFactory(redisConnectionFactory);
            // or: JedisPool pool = new JedisPool(redis.getHost(), redis.getPort())
            log.warn("Redis provider configured but full Redis integration pending - using in-memory for now");
        } catch (Exception e) {
            log.error("Failed to initialize Redis for feedback learning, using in-memory: {}", e.getMessage());
        }
    }
    
    /**
     * Records feedback for a Q&A interaction.
     */
    @Transactional
    public void recordFeedback(Long interactionId, boolean isHelpful, String feedbackText) {
        QAInteraction interaction = qaInteractionRepository.findById(interactionId)
                .orElseThrow(() -> new IllegalArgumentException("Q&A interaction not found: " + interactionId));
        
        // Update interaction with feedback
        interaction.setFeedbackHelpful(isHelpful);
        interaction.setFeedbackText(feedbackText);
        interaction.setFeedbackAt(LocalDateTime.now());
        qaInteractionRepository.save(interaction);
        
        // Update learning statistics
        updateQueryPatternStats(interaction.getQuestion(), isHelpful);
        updateSourceRelevanceStats(interaction.getSourceKnowledgeIds(), isHelpful);
        
        log.info("Recorded feedback for interaction {}: helpful={}", interactionId, isHelpful);
    }
    
    /**
     * Updates statistics for query patterns.
     */
    private void updateQueryPatternStats(String query, boolean isHelpful) {
        if (query == null || query.isEmpty()) return;
        
        // Extract key terms (simple approach - could use NLP)
        String pattern = extractQueryPattern(query);
        
        queryPatternStats.compute(pattern, (k, stats) -> {
            if (stats == null) {
                stats = new FeedbackStats();
            }
            stats.addFeedback(isHelpful);
            return stats;
        });
    }
    
    /**
     * Updates relevance statistics for knowledge sources.
     */
    private void updateSourceRelevanceStats(String sourceIds, boolean isHelpful) {
        if (sourceIds == null || sourceIds.isEmpty()) return;
        
        String[] ids = sourceIds.split(",");
        for (String id : ids) {
            sourceStats.compute(id.trim(), (k, stats) -> {
                if (stats == null) {
                    stats = new SourceRelevanceStats();
                }
                stats.addFeedback(isHelpful);
                return stats;
            });
        }
    }
    
    /**
     * Extracts a pattern from the query for learning.
     */
    private String extractQueryPattern(String query) {
        String normalized = query.toLowerCase().trim();
        
        // Extract question type
        if (normalized.startsWith("what")) return "what-question";
        if (normalized.startsWith("how")) return "how-question";
        if (normalized.startsWith("why")) return "why-question";
        if (normalized.startsWith("who")) return "who-question";
        if (normalized.startsWith("when")) return "when-question";
        if (normalized.startsWith("which")) return "which-question";
        
        // Extract domain
        if (normalized.contains("risk")) return "risk-query";
        if (normalized.contains("status")) return "status-query";
        if (normalized.contains("progress")) return "progress-query";
        if (normalized.contains("team")) return "team-query";
        
        return "general-query";
    }
    
    /**
     * Gets the success rate for a query pattern.
     */
    public double getPatternSuccessRate(String query) {
        String pattern = extractQueryPattern(query);
        FeedbackStats stats = queryPatternStats.get(pattern);
        return stats != null ? stats.getSuccessRate() : 0.5; // Default 50%
    }
    
    /**
     * Gets the relevance boost for a knowledge source based on feedback.
     */
    public double getSourceRelevanceBoost(String sourceId) {
        SourceRelevanceStats stats = sourceStats.get(sourceId);
        if (stats == null) return 1.0; // No boost by default
        
        // Boost up to 1.2x for highly successful sources, down to 0.8x for poor ones
        double successRate = stats.getSuccessRate();
        return 0.8 + (successRate * 0.4);
    }
    
    /**
     * Gets insights about poorly performing queries.
     */
    public Map<String, FeedbackStats> getPoorlyPerformingPatterns() {
        return queryPatternStats.entrySet().stream()
                .filter(e -> e.getValue().getSuccessRate() < 0.5 && e.getValue().getTotalCount() >= 5)
                .collect(java.util.stream.Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
    
    /**
     * Statistics for query pattern feedback.
     */
    public static class FeedbackStats {
        private int helpfulCount = 0;
        private int unhelpfulCount = 0;
        
        public void addFeedback(boolean isHelpful) {
            if (isHelpful) {
                helpfulCount++;
            } else {
                unhelpfulCount++;
            }
        }
        
        public double getSuccessRate() {
            int total = getTotalCount();
            return total > 0 ? (double) helpfulCount / total : 0.0;
        }
        
        public int getTotalCount() {
            return helpfulCount + unhelpfulCount;
        }
        
        public int getHelpfulCount() {
            return helpfulCount;
        }
        
        public int getUnhelpfulCount() {
            return unhelpfulCount;
        }
    }
    
    /**
     * Statistics for source relevance feedback.
     */
    public static class SourceRelevanceStats {
        private int helpfulCount = 0;
        private int unhelpfulCount = 0;
        
        public void addFeedback(boolean isHelpful) {
            if (isHelpful) {
                helpfulCount++;
            } else {
                unhelpfulCount++;
            }
        }
        
        public double getSuccessRate() {
            int total = helpfulCount + unhelpfulCount;
            return total > 0 ? (double) helpfulCount / total : 0.5;
        }
    }
}
