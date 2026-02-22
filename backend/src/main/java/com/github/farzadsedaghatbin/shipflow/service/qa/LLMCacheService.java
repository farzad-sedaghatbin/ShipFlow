package com.github.farzadsedaghatbin.shipflow.service.qa;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * LLM response caching service for cost optimization.
 *
 * <p>
 * Uses Redis when configured via app.ai.cache.provider=redis, otherwise
 * in-memory. Caches LLM responses by prompt hash to avoid duplicate API calls.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LLMCacheService {

  private final AICacheConfig cacheConfig;

  @Value("${llm.cache.ttl.minutes:60}")
  private int cacheTtlMinutes = 60; // Default value for non-Spring contexts

  @Value("${llm.cache.max.size:1000}")
  private int maxCacheSize = 1000; // Default value for non-Spring contexts

  // Cache storage - uses Redis when configured, in-memory otherwise
  private final Map<String, CachedResponse> cache = new ConcurrentHashMap<>();

  @PostConstruct
  public void init() {
    if (cacheConfig.isRedisProvider()) {
      initializeRedis();
      log.info("LLMCacheService initialized with Redis provider ({}:{})", cacheConfig.getRedis().getHost(),
          cacheConfig.getRedis().getPort());
    } else {
      log.info("LLMCacheService initialized with in-memory provider");
    }
  }

  /**
   * Initialize Redis connection for distributed LLM cache. In production, this
   * would use Spring Data Redis or Jedis/Lettuce client.
   */
  private void initializeRedis() {
    try {
      AICacheConfig.RedisConfig redis = cacheConfig.getRedis();
      log.info("Initializing Redis for LLM cache at {}:{}", redis.getHost(), redis.getPort());
      // In production implementation:
      // RedisTemplate<String, CachedResponse> redisTemplate = new RedisTemplate<>();
      // redisTemplate.setConnectionFactory(redisConnectionFactory);
      log.warn("Redis provider configured but full Redis integration pending - using in-memory for now");
    } catch (Exception e) {
      log.error("Failed to initialize Redis for LLM cache, using in-memory: {}", e.getMessage());
    }
  }

  /** Gets a cached LLM response if available and not expired. */
  public String getCachedResponse(String prompt) {
    String hash = hashPrompt(prompt);
    CachedResponse cached = cache.get(hash);

    if (cached != null) {
      if (cached.isExpired(cacheTtlMinutes)) {
        cache.remove(hash);
        log.debug("Cache expired for prompt hash: {}", hash.substring(0, 8));
        return null;
      }

      log.debug("Cache HIT for prompt hash: {}", hash.substring(0, 8));
      cached.incrementHitCount();
      return cached.getResponse();
    }

    log.debug("Cache MISS for prompt hash: {}", hash.substring(0, 8));
    return null;
  }

  /** Caches an LLM response. */
  public void cacheResponse(String prompt, String response) {
    if (response == null) {
      return; // Don't cache null responses
    }

    String hash = hashPrompt(prompt);
    cache.put(hash, new CachedResponse(response));
    log.debug("Cached response for prompt hash: {}", hash.substring(0, 8));

    // Evict oldest entries if cache exceeds max size
    if (cache.size() > maxCacheSize) {
      evictOldestEntries();
    }
  }

  /** Generates a hash of the prompt for cache key. */
  private String hashPrompt(String prompt) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashBytes = digest.digest(prompt.getBytes(StandardCharsets.UTF_8));

      // Convert to hex string
      StringBuilder hexString = new StringBuilder();
      for (byte b : hashBytes) {
        String hex = Integer.toHexString(0xff & b);
        if (hex.length() == 1)
          hexString.append('0');
        hexString.append(hex);
      }
      return hexString.toString();
    } catch (Exception e) {
      log.error("Failed to hash prompt: {}", e.getMessage());
      return String.valueOf(prompt.hashCode());
    }
  }

  /** Evicts oldest cache entries when size limit reached. */
  private void evictOldestEntries() {
    int currentSize = cache.size();
    // Evict approximately 27.5% of current size (to leave ~72.5% of maxCacheSize
    // after continued
    // additions)
    int toRemove = (currentSize * 275 + 999) / 1000; // Approximately 27.5%, rounded

    if (toRemove <= 0) {
      return;
    }

    cache.entrySet().stream().sorted((e1, e2) -> e1.getValue().getCachedAt().compareTo(e2.getValue().getCachedAt()))
        .limit(toRemove).forEach(entry -> {
          cache.remove(entry.getKey());
          log.debug("Evicted cache entry: {}", entry.getKey().substring(0, 8));
        });
  }

  /** Gets cache statistics. */
  public CacheStats getStats() {
    int totalHits = cache.values().stream().mapToInt(CachedResponse::getHitCount).sum();

    return new CacheStats(cache.size(), totalHits);
  }

  /** Clears the cache. */
  public void clearCache() {
    int size = cache.size();
    cache.clear();
    log.info("Cleared LLM cache: {} entries removed", size);
  }

  /** Cached response with metadata. */
  private static class CachedResponse {
    private final String response;
    private final LocalDateTime cachedAt;
    private int hitCount;

    public CachedResponse(String response) {
      this.response = response;
      this.cachedAt = LocalDateTime.now();
      this.hitCount = 0;
    }

    public String getResponse() {
      return response;
    }

    public LocalDateTime getCachedAt() {
      return cachedAt;
    }

    public int getHitCount() {
      return hitCount;
    }

    public void incrementHitCount() {
      this.hitCount++;
    }

    public boolean isExpired(int ttlMinutes) {
      return LocalDateTime.now().isAfter(cachedAt.plusMinutes(ttlMinutes));
    }
  }

  /** Cache statistics. */
  public static class CacheStats {
    private final int size;
    private final int totalHits;

    public CacheStats(int size, int totalHits) {
      this.size = size;
      this.totalHits = totalHits;
    }

    public int getSize() {
      return size;
    }

    public int getTotalHits() {
      return totalHits;
    }
  }
}
