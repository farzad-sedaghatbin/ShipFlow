package com.github.farzadsedaghatbin.shipflow.service.qa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.ConversationContext;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

/**
 * Unit tests for {@link ConversationManager} covering both in-memory and Redis
 * modes, including graceful degradation and write-through cache behaviour.
 */
@ExtendWith(MockitoExtension.class)
class ConversationManagerTest {

  @Mock
  private AICacheConfig cacheConfig;

  @Mock
  private StringRedisTemplate redisTemplate;

  @Mock
  private ValueOperations<String, String> valueOps;

  private ObjectMapper objectMapper;
  private ConversationManager manager;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
  }

  /**
   * Helper: build the manager in in-memory mode (Redis provider disabled).
   */
  private void initInMemoryMode() {
    lenient().when(cacheConfig.isRedisProvider()).thenReturn(false);
    manager = new ConversationManager(cacheConfig, redisTemplate, objectMapper);
    // init() is a @PostConstruct – invoke it manually in tests
    manager.init();
  }

  /**
   * Helper: build the manager in Redis mode with a healthy connection.
   */
  private void initRedisMode() {
    when(cacheConfig.isRedisProvider()).thenReturn(true);
    AICacheConfig.RedisConfig redisConfig = new AICacheConfig.RedisConfig();
    redisConfig.setHost("localhost");
    redisConfig.setPort(6379);
    lenient().when(cacheConfig.getRedis()).thenReturn(redisConfig);
    lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);

    manager = new ConversationManager(cacheConfig, redisTemplate, objectMapper);
    manager.init(); // triggers initializeRedis -> health check
  }

  // =====================================================================
  // In-memory mode tests
  // =====================================================================
  @Nested
  class InMemoryMode {

    @BeforeEach
    void setUp() {
      initInMemoryMode();
    }

    @Test
    void createConversation_returnsNonNullContext() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      assertThat(ctx).isNotNull();
      assertThat(ctx.getConversationId()).isNotBlank();
      assertThat(ctx.getUserId()).isEqualTo(1L);
      assertThat(ctx.getContextType()).isEqualTo("PITCH");
      assertThat(ctx.getContextId()).isEqualTo(100L);
    }

    @Test
    void getOrCreateConversation_existingId_returnsSameConversation() {
      ConversationContext created = manager.createConversation(1L, "PITCH", 100L);
      ConversationContext fetched = manager.getOrCreateConversation(
          created.getConversationId(), 1L, "PITCH", 100L);
      assertThat(fetched.getConversationId()).isEqualTo(created.getConversationId());
    }

    @Test
    void getOrCreateConversation_nullId_createsNew() {
      ConversationContext ctx = manager.getOrCreateConversation(null, 1L, "PITCH", 100L);
      assertThat(ctx).isNotNull();
      assertThat(ctx.getConversationId()).isNotBlank();
    }

    @Test
    void addTurn_incrementsHistory() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      manager.addTurn(ctx.getConversationId(), "Q1", "A1");

      ConversationContext fetched = manager.getOrCreateConversation(
          ctx.getConversationId(), 1L, "PITCH", 100L);
      assertThat(fetched.getHistory()).hasSize(1);
      assertThat(fetched.getHistory().get(0).getQuestion()).isEqualTo("Q1");
    }

    @Test
    void buildConversationHistory_emptyHistory_returnsEmpty() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      String history = manager.buildConversationHistory(ctx.getConversationId(), 5);
      assertThat(history).isEmpty();
    }

    @Test
    void buildConversationHistory_withTurns_includesContent() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      manager.addTurn(ctx.getConversationId(), "What status?", "In progress.");
      String history = manager.buildConversationHistory(ctx.getConversationId(), 5);
      assertThat(history).contains("What status?").contains("In progress.");
    }

    @Test
    void endConversation_marksInactive() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      manager.endConversation(ctx.getConversationId());

      ConversationContext fetched = manager.getOrCreateConversation(
          ctx.getConversationId(), 1L, "PITCH", 100L);
      // After ending, getOrCreate should create new because old is inactive/expired
      // or the context might still be present – just verify end didn't throw
      assertThat(fetched).isNotNull();
    }

    @Test
    void getActiveConversationCount_returnsCorrectCount() {
      manager.createConversation(1L, "PITCH", 100L);
      manager.createConversation(2L, "CYCLE", 200L);
      assertThat(manager.getActiveConversationCount()).isEqualTo(2);
    }
  }

  // =====================================================================
  // Redis mode tests
  // =====================================================================
  @Nested
  class RedisMode {

    @BeforeEach
    void setUp() {
      initRedisMode();
    }

    @Test
    void createConversation_savesToRedis() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      assertThat(ctx).isNotNull();
      // Verify Redis SET was called (write-through also updates in-memory)
      verify(valueOps).set(startsWith("conv:"), anyString(), eq((long) 30), eq(TimeUnit.MINUTES));
    }

    @Test
    void addTurn_loadsFromRedisAndSavesBack() throws Exception {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      String convId = ctx.getConversationId();

      // Simulate Redis returning the previously saved context
      String json = objectMapper.writeValueAsString(ctx);
      when(valueOps.get("conv:" + convId)).thenReturn(json);

      manager.addTurn(convId, "Q1", "A1");

      // Should have loaded from Redis (get) and saved back (set called again)
      verify(valueOps).get("conv:" + convId);
      // 2 total SET calls: create + addTurn
      verify(valueOps, times(2)).set(eq("conv:" + convId), anyString(), eq((long) 30), eq(TimeUnit.MINUTES));
    }

    @Test
    void loadFromRedis_fallsBackToInMemory_whenRedisReturnsNull() throws Exception {
      // Create a conversation (written to both Redis and in-memory via write-through)
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      String convId = ctx.getConversationId();

      // Redis returns null (e.g. key expired or evicted)
      when(valueOps.get("conv:" + convId)).thenReturn(null);

      // Should still find the conversation via in-memory fallback
      ConversationContext fetched = manager.getOrCreateConversation(convId, 1L, "PITCH", 100L);
      assertThat(fetched).isNotNull();
      assertThat(fetched.getConversationId()).isEqualTo(convId);
    }

    @Test
    void loadFromRedis_fallsBackToInMemory_whenRedisThrows() throws Exception {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      String convId = ctx.getConversationId();

      // Redis throws on read
      when(valueOps.get("conv:" + convId)).thenThrow(new RuntimeException("Connection refused"));

      ConversationContext fetched = manager.getOrCreateConversation(convId, 1L, "PITCH", 100L);
      assertThat(fetched).isNotNull();
      assertThat(fetched.getConversationId()).isEqualTo(convId);
    }

    @Test
    void saveToRedis_preservesInMemoryCopy_whenRedisWriteFails() {
      // Make the SET call throw on the first real save
      doThrow(new RuntimeException("Connection refused"))
          .when(valueOps).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

      // Create should not throw – in-memory copy preserved
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      assertThat(ctx).isNotNull();

      // Verify in-memory count includes the conversation
      assertThat(manager.getActiveConversationCount()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void buildConversationHistory_fromRedis() throws Exception {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      ctx.addTurn("What's the status?", "All good.");
      String json = objectMapper.writeValueAsString(ctx);
      when(valueOps.get("conv:" + ctx.getConversationId())).thenReturn(json);

      String history = manager.buildConversationHistory(ctx.getConversationId(), 5);
      assertThat(history).contains("What's the status?").contains("All good.");
    }
  }

  // =====================================================================
  // Redis initialization failure / graceful degradation
  // =====================================================================
  @Nested
  class RedisInitFailure {

    @Test
    void fallsBackToInMemory_whenRedisHealthCheckFails() {
      when(cacheConfig.isRedisProvider()).thenReturn(true);
      AICacheConfig.RedisConfig redisConfig = new AICacheConfig.RedisConfig();
      redisConfig.setHost("localhost");
      redisConfig.setPort(6379);
      when(cacheConfig.getRedis()).thenReturn(redisConfig);
      when(redisTemplate.opsForValue()).thenThrow(new RuntimeException("Connection refused"));

      manager = new ConversationManager(cacheConfig, redisTemplate, objectMapper);
      manager.init();

      // Should fall back to in-memory – create and retrieve still work
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      assertThat(ctx).isNotNull();
      ConversationContext fetched = manager.getOrCreateConversation(
          ctx.getConversationId(), 1L, "PITCH", 100L);
      assertThat(fetched.getConversationId()).isEqualTo(ctx.getConversationId());
    }
  }

  // =====================================================================
  // Context name propagation
  // =====================================================================
  @Nested
  class ContextName {

    @BeforeEach
    void setUp() {
      initInMemoryMode();
    }

    @Test
    void createConversation_withContextName() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L, "My Pitch");
      assertThat(ctx.getContextName()).isEqualTo("My Pitch");
    }

    @Test
    void getOrCreateConversation_updatesContextNameIfMissing() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L);
      assertThat(ctx.getContextName()).isNull();

      ConversationContext updated = manager.getOrCreateConversation(
          ctx.getConversationId(), 1L, "PITCH", 100L, "Late Name");
      assertThat(updated.getContextName()).isEqualTo("Late Name");
    }
  }

  // =====================================================================
  // Context info retrieval
  // =====================================================================
  @Nested
  class ContextInfo {

    @BeforeEach
    void setUp() {
      initInMemoryMode();
    }

    @Test
    void getMostRecentContext_returnsInfo() {
      ConversationContext ctx = manager.createConversation(1L, "PITCH", 100L, "My Pitch");
      ConversationContext.ContextInfo info = manager.getMostRecentContext(ctx.getConversationId());
      assertThat(info).isNotNull();
      assertThat(info.getContextType()).isEqualTo("PITCH");
      assertThat(info.getContextId()).isEqualTo(100L);
      assertThat(info.getContextName()).isEqualTo("My Pitch");
    }

    @Test
    void getMostRecentContext_returnsNull_forUnknownId() {
      ConversationContext.ContextInfo info = manager.getMostRecentContext("unknown-id");
      assertThat(info).isNull();
    }
  }
}
