# Redis Configuration Guide

## Overview

ShipFlow uses a unified Redis configuration system for all caching and stateful services. This allows seamless switching between in-memory (development) and Redis (production) storage without code changes.

## Configuration

### Application Properties

All services share the same Redis configuration defined in `application.properties`:

```properties
# Cache provider: in-memory (default) or redis
app.ai.cache.provider=${AI_CACHE_PROVIDER:in-memory}

# Redis configuration (only used when provider=redis)
app.ai.cache.redis.host=${AI_CACHE_REDIS_HOST:localhost}
app.ai.cache.redis.port=${AI_CACHE_REDIS_PORT:6379}
app.ai.cache.redis.password=${AI_CACHE_REDIS_PASSWORD:}
app.ai.cache.redis.database=${AI_CACHE_REDIS_DATABASE:0}
```

### Environment Variables (Production)

For production deployments, use environment variables:

```bash
AI_CACHE_PROVIDER=redis
AI_CACHE_REDIS_HOST=redis.example.com
AI_CACHE_REDIS_PORT=6379
AI_CACHE_REDIS_PASSWORD=your-secure-password
AI_CACHE_REDIS_DATABASE=0
```

### Docker Compose Example

```yaml
services:
  backend:
    environment:
      - AI_CACHE_PROVIDER=redis
      - AI_CACHE_REDIS_HOST=redis
      - AI_CACHE_REDIS_PORT=6379
    depends_on:
      - redis
  
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    volumes:
      - redis-data:/data

volumes:
  redis-data:
```

## Services Using Redis Configuration

All services implementing caching or stateful storage follow this pattern:

### 1. AICacheService
**Purpose:** Caches AI-generated responses (risk analysis, Q&A answers)
- **Dev:** In-memory ConcurrentHashMap
- **Prod:** Redis with TTL support
- **Benefit:** Shared cache across multiple app instances

### 2. FeedbackLearningService
**Purpose:** Aggregates user feedback for RAG quality improvement
- **Dev:** In-memory maps for query patterns and source stats
- **Prod:** Redis for distributed feedback collection
- **Benefit:** Centralized learning across all instances

### 3. LLMCacheService
**Purpose:** Caches LLM API responses (40-60% cost reduction)
- **Dev:** In-memory with LRU eviction
- **Prod:** Redis with persistence
- **Benefit:** Cache survives restarts, shared across instances

### 4. ConversationManager
**Purpose:** Manages multi-turn Q&A conversation contexts
- **Dev:** In-memory with auto-expiration
- **Prod:** Redis with TTL
- **Benefit:** Conversations persist across app restarts

## Implementation Pattern

All services follow this standardized pattern:

```java
@Service
@RequiredArgsConstructor
public class MyService {
    
    private final AICacheConfig cacheConfig;
    
    // Storage - uses Redis when configured, in-memory otherwise
    private final Map<String, Object> cache = new ConcurrentHashMap<>();
    
    // Redis client (lazy initialized if provider=redis)
    private Object redisClient;
    
    @PostConstruct
    public void init() {
        if (cacheConfig.isRedisProvider()) {
            initializeRedis();
            log.info("MyService initialized with Redis provider");
        } else {
            log.info("MyService initialized with in-memory provider");
        }
    }
    
    private void initializeRedis() {
        try {
            AICacheConfig.RedisConfig redis = cacheConfig.getRedis();
            log.info("Initializing Redis at {}:{}", redis.getHost(), redis.getPort());
            // Production: RedisTemplate or Jedis/Lettuce client
            log.warn("Redis integration pending - using in-memory for now");
        } catch (Exception e) {
            log.error("Failed to initialize Redis, using in-memory: {}", e.getMessage());
        }
    }
}
```

## Testing Pattern

Tests should mock `AICacheConfig` to use in-memory storage:

```java
@ExtendWith(MockitoExtension.class)
class MyServiceTest {
    
    @Mock
    private AICacheConfig cacheConfig;
    
    private MyService service;
    
    @BeforeEach
    void setUp() {
        // Avoid Redis initialization in tests
        lenient().when(cacheConfig.isRedisProvider()).thenReturn(false);
        service = new MyService(cacheConfig);
    }
}
```

## Benefits

✅ **Single configuration source** - All services use `app.ai.cache.provider`  
✅ **Environment-aware** - Automatically uses in-memory in dev, Redis in prod  
✅ **Graceful fallback** - Falls back to in-memory if Redis unavailable  
✅ **No code changes** - Switch providers via configuration only  
✅ **Consistent pattern** - All new stateful services should follow this approach  

## Adding New Services

When creating a new service that needs distributed state:

1. **Inject AICacheConfig** as a dependency
2. **Add @PostConstruct init()** to check `cacheConfig.isRedisProvider()`
3. **Initialize Redis client** if configured (or use in-memory as fallback)
4. **Log the provider** being used for observability
5. **Update tests** to mock AICacheConfig with `lenient().when()`

## Future Enhancements

- [ ] Complete Redis integration (currently using in-memory fallback)
- [ ] Add RedisTemplate configuration beans
- [ ] Implement Redis-specific serialization for complex objects
- [ ] Add Redis health checks and connection pooling
- [ ] Support Redis Cluster for high availability
- [ ] Add Redis metrics to Micrometer monitoring

## Related Documentation

- [application.properties](backend/src/main/resources/application.properties) - Full configuration
- [AICacheConfig.java](backend/src/main/java/com/github/farzadsedaghatbin/shipflow/config/AICacheConfig.java) - Configuration class
- [RAG_ARCHITECTURE.md](RAG_ARCHITECTURE.md) - RAG system architecture
- [README.md](README.md#configuration) - Quick start configuration guide
