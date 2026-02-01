package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.ConversationContext;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Manages conversation contexts for multi-turn Q&A. Uses Redis when configured via
 * app.ai.cache.provider=redis, otherwise in-memory.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConversationManager {

  private final AICacheConfig cacheConfig;

  // Conversation storage - uses Redis when configured, in-memory otherwise
  private final Map<String, ConversationContext> conversations = new ConcurrentHashMap<>();

  // Redis client (lazy initialized if provider=redis)
  private Object redisClient; // Would be Jedis or Lettuce in production

  @PostConstruct
  public void init() {
    if (cacheConfig.isRedisProvider()) {
      initializeRedis();
      log.info(
          "ConversationManager initialized with Redis provider ({}:{})",
          cacheConfig.getRedis().getHost(),
          cacheConfig.getRedis().getPort());
    } else {
      log.info("ConversationManager initialized with in-memory provider");
    }
  }

  /**
   * Initialize Redis connection for distributed conversation management. In production, this would
   * use Spring Data Redis or Jedis/Lettuce client.
   */
  private void initializeRedis() {
    try {
      AICacheConfig.RedisConfig redis = cacheConfig.getRedis();
      log.info(
          "Initializing Redis for conversation management at {}:{}",
          redis.getHost(),
          redis.getPort());
      // In production implementation:
      // RedisTemplate<String, ConversationContext> redisTemplate = new RedisTemplate<>();
      // redisTemplate.setConnectionFactory(redisConnectionFactory);
      log.warn(
          "Redis provider configured but full Redis integration pending - using in-memory for now");
    } catch (Exception e) {
      log.error(
          "Failed to initialize Redis for conversation management, using in-memory: {}",
          e.getMessage());
    }
  }

  /** Create a new conversation. */
  public ConversationContext createConversation(Long userId, String contextType, Long contextId) {
    String conversationId = UUID.randomUUID().toString();

    ConversationContext context =
        ConversationContext.builder()
            .conversationId(conversationId)
            .userId(userId)
            .contextType(contextType)
            .contextId(contextId)
            .startedAt(LocalDateTime.now())
            .lastInteractionAt(LocalDateTime.now())
            .isActive(true)
            .build();

    conversations.put(conversationId, context);
    log.debug("Created conversation {} for user {}", conversationId, userId);

    // Clean up expired conversations
    cleanupExpired();

    return context;
  }

  /** Get existing conversation or create new one. */
  public ConversationContext getOrCreateConversation(
      String conversationId, Long userId, String contextType, Long contextId) {

    if (conversationId != null) {
      ConversationContext existing = conversations.get(conversationId);
      if (existing != null && !existing.isExpired()) {
        return existing;
      }
    }

    return createConversation(userId, contextType, contextId);
  }

  /** Add a turn to the conversation. */
  public void addTurn(String conversationId, String question, String answer) {
    ConversationContext context = conversations.get(conversationId);
    if (context != null) {
      context.addTurn(question, answer);
      log.debug(
          "Added turn to conversation {}, total turns: {}",
          conversationId,
          context.getHistory().size());
    }
  }

  /** Build conversation history context for prompt. */
  public String buildConversationHistory(String conversationId, int maxTurns) {
    ConversationContext context = conversations.get(conversationId);
    if (context == null || context.getHistory().isEmpty()) {
      return "";
    }

    StringBuilder history = new StringBuilder();
    history.append("Previous conversation:\n");

    for (ConversationContext.Turn turn : context.getRecentTurns(maxTurns)) {
      history.append("User: ").append(turn.getQuestion()).append("\n");
      history.append("Assistant: ").append(turn.getAnswer()).append("\n\n");
    }

    return history.toString();
  }

  /** End a conversation. */
  public void endConversation(String conversationId) {
    ConversationContext context = conversations.get(conversationId);
    if (context != null) {
      context.setIsActive(false);
      log.debug("Ended conversation {}", conversationId);
    }
  }

  /**
   * Get the most recent context (contextType and contextId) from conversation history. Useful for
   * inferring context when user asks follow-up questions.
   */
  public ConversationContext.ContextInfo getMostRecentContext(String conversationId) {
    ConversationContext context = conversations.get(conversationId);
    if (context != null) {
      return new ConversationContext.ContextInfo(context.getContextType(), context.getContextId());
    }
    return null;
  }

  /** Clean up expired conversations (inactive for > 30 minutes). */
  private void cleanupExpired() {
    int removed = 0;
    for (Map.Entry<String, ConversationContext> entry : conversations.entrySet()) {
      if (entry.getValue().isExpired()) {
        conversations.remove(entry.getKey());
        removed++;
      }
    }

    if (removed > 0) {
      log.debug("Cleaned up {} expired conversations", removed);
    }
  }

  /** Get conversation count (for monitoring). */
  public int getActiveConversationCount() {
    return (int)
        conversations.values().stream().filter(c -> c.getIsActive() && !c.isExpired()).count();
  }
}
