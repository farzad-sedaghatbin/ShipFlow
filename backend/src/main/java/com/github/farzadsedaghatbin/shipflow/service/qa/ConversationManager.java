package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.ConversationContext;
import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Manages conversation contexts for multi-turn Q&A. Uses Redis when configured
 * via app.ai.cache.provider=redis, otherwise in-memory.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class ConversationManager {

  private final AICacheConfig cacheConfig;
  private final StringRedisTemplate redisTemplate;

  // The primary (auto-configured) ObjectMapper is used here intentionally.
  // It has JSR-310 and date formatting configured via application.properties.
  // Default typing is not needed because we specify target classes explicitly.
  private final ObjectMapper objectMapper;

  // In-memory fallback / in-memory mode storage
  private final Map<String, ConversationContext> conversations = new ConcurrentHashMap<>();

  // Tracks whether Redis is actually reachable; allows graceful fallback to in-memory
  private volatile boolean redisAvailable;

  private static final String CONV_KEY_PREFIX = "conv:";
  private static final int CONV_TTL_MINUTES = 30;

  private boolean isRedisActive() {
    return cacheConfig.isRedisProvider() && redisAvailable;
  }

  @PostConstruct
  public void init() {
    if (cacheConfig.isRedisProvider()) {
      initializeRedis();
      log.info("ConversationManager initialized with Redis provider ({}:{}){}",
          cacheConfig.getRedis().getHost(), cacheConfig.getRedis().getPort(),
          redisAvailable ? "" : " [FALLBACK to in-memory]");
    } else {
      log.info("ConversationManager initialized with in-memory provider");
    }
  }

  private void initializeRedis() {
    try {
      AICacheConfig.RedisConfig redis = cacheConfig.getRedis();
      log.info("Initializing Redis for conversation management at {}:{}", redis.getHost(), redis.getPort());
      redisTemplate.opsForValue().set("shipflow:conv-health", "ok");
      redisTemplate.delete("shipflow:conv-health");
      redisAvailable = true;
      log.info("ConversationManager Redis connection verified");
    } catch (Exception e) {
      redisAvailable = false;
      log.error("Failed to connect to Redis for ConversationManager, falling back to in-memory: {}",
          e.getMessage());
    }
  }

  private void saveToRedis(String conversationId, ConversationContext context) {
    try {
      String json = objectMapper.writeValueAsString(context);
      redisTemplate.opsForValue().set(CONV_KEY_PREFIX + conversationId, json, CONV_TTL_MINUTES, TimeUnit.MINUTES);
    } catch (Exception e) {
      log.warn("Failed to save conversation to Redis: {}", e.getMessage());
      conversations.put(conversationId, context);
    }
  }

  private ConversationContext loadFromRedis(String conversationId) {
    try {
      String json = redisTemplate.opsForValue().get(CONV_KEY_PREFIX + conversationId);
      if (json == null || json.isBlank()) return null;
      return objectMapper.readValue(json, ConversationContext.class);
    } catch (Exception e) {
      log.warn("Failed to load conversation from Redis: {}", e.getMessage());
      // Fail fast: do not return stale in-memory data that may differ from Redis state
      return null;
    }
  }

  /** Create a new conversation. */
  public ConversationContext createConversation(Long userId, String contextType, Long contextId) {
    return createConversation(userId, contextType, contextId, null);
  }

  /** Create a new conversation with optional context name. */
  public ConversationContext createConversation(Long userId, String contextType, Long contextId,
      String contextName) {
    String conversationId = UUID.randomUUID().toString();

    ConversationContext context = ConversationContext.builder().conversationId(conversationId).userId(userId)
        .contextType(contextType).contextId(contextId).contextName(contextName).startedAt(LocalDateTime.now())
        .lastInteractionAt(LocalDateTime.now()).isActive(true).build();

    if (isRedisActive()) {
      saveToRedis(conversationId, context);
    } else {
      conversations.put(conversationId, context);
      cleanupExpired();
    }
    log.debug("Created conversation {} for user {} (contextName='{}')", conversationId, userId, contextName);

    return context;
  }

  /** Get existing conversation or create new one. */
  public ConversationContext getOrCreateConversation(String conversationId, Long userId, String contextType,
      Long contextId) {
    return getOrCreateConversation(conversationId, userId, contextType, contextId, null);
  }

  /** Get existing conversation or create new one with optional context name. */
  public ConversationContext getOrCreateConversation(String conversationId, Long userId, String contextType,
      Long contextId, String contextName) {

    if (conversationId != null) {
      ConversationContext existing = isRedisActive()
          ? loadFromRedis(conversationId) : conversations.get(conversationId);
      if (existing != null && !existing.isExpired()) {
        // Update context name if newly provided
        if (contextName != null && existing.getContextName() == null) {
          existing.setContextName(contextName);
        }
        return existing;
      }
    }

    return createConversation(userId, contextType, contextId, contextName);
  }

  /** Add a turn to the conversation. */
  public void addTurn(String conversationId, String question, String answer) {
    ConversationContext context = isRedisActive()
        ? loadFromRedis(conversationId) : conversations.get(conversationId);
    if (context != null) {
      context.addTurn(question, answer);
      if (isRedisActive()) {
        saveToRedis(conversationId, context);
      }
      log.debug("Added turn to conversation {}, total turns: {}", conversationId, context.getHistory().size());
    }
  }

  /** Build conversation history context for prompt. */
  public String buildConversationHistory(String conversationId, int maxTurns) {
    ConversationContext context = isRedisActive()
        ? loadFromRedis(conversationId) : conversations.get(conversationId);
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
    ConversationContext context = isRedisActive()
        ? loadFromRedis(conversationId) : conversations.get(conversationId);
    if (context != null) {
      context.setIsActive(false);
      if (isRedisActive()) {
        saveToRedis(conversationId, context);
      }
      log.debug("Ended conversation {}", conversationId);
    }
  }

  /**
   * Get the most recent context (contextType and contextId) from conversation
   * history. Useful for inferring context when user asks follow-up questions.
   */
  public ConversationContext.ContextInfo getMostRecentContext(String conversationId) {
    ConversationContext context = isRedisActive()
        ? loadFromRedis(conversationId) : conversations.get(conversationId);
    if (context != null) {
      return new ConversationContext.ContextInfo(context.getContextType(), context.getContextId(),
          context.getContextName());
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
    return (int) conversations.values().stream().filter(c -> c.getIsActive() && !c.isExpired()).count();
  }
}
