package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.AICacheConfig;
import com.github.farzadsedaghatbin.shipflow.dto.qa.QAResponse;
import com.github.farzadsedaghatbin.shipflow.dto.risk.CycleRiskOverviewDTO;
import com.github.farzadsedaghatbin.shipflow.dto.risk.PitchRiskDTO;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Server-side caching service for AI responses. Provides intelligent caching to reduce AI API calls
 * across all users.
 *
 * <p>Features: - TTL-based cache expiration (0 = infinite until invalidation or restart) -
 * Automatic invalidation when data changes - Fuzzy matching for similar Q&A questions - Feature
 * flags for enabling/disabling caching per feature - Thread-safe concurrent access - Supports
 * in-memory (dev) and Redis (production) providers
 *
 * <p>For risk analysis with change detection, TTL=0 (infinite) is recommended since cache is
 * automatically invalidated when data changes.
 */
@Service
@Slf4j
@ConditionalOnProperty(
    name = "app.ai.risk-analysis.enabled",
    havingValue = "true",
    matchIfMissing = false)
public class AICacheService {

  private final AICacheConfig cacheConfig;

  // In-memory cache storage - thread-safe maps
  private final ConcurrentHashMap<String, CacheEntry<PitchRiskDTO>> pitchRiskCache =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CacheEntry<CycleRiskOverviewDTO>> cycleRiskCache =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, CacheEntry<QAResponse>> qaCache =
      new ConcurrentHashMap<>();

  // Track data versions for change detection
  private final ConcurrentHashMap<Long, DataVersion> pitchDataVersions = new ConcurrentHashMap<>();

  // Redis client (lazy initialized if needed)
  private Object redisClient; // Would be Jedis or Lettuce in real implementation

  public AICacheService(AICacheConfig cacheConfig) {
    this.cacheConfig = cacheConfig;
  }

  @PostConstruct
  public void init() {
    cacheConfig.logConfiguration();

    if (cacheConfig.isRedisProvider()) {
      initializeRedis();
    }

    log.info("AI Cache Service initialized with provider: {}", cacheConfig.getProvider());
  }

  @PreDestroy
  public void shutdown() {
    if (redisClient != null) {
      // Close Redis connection
      log.info("Closing Redis connection");
    }
  }

  /**
   * Initialize Redis connection if configured. Note: This is a placeholder - actual implementation
   * would use Jedis or Lettuce.
   */
  private void initializeRedis() {
    try {
      AICacheConfig.RedisConfig redis = cacheConfig.getRedis();
      log.info("Initializing Redis cache at {}:{}", redis.getHost(), redis.getPort());
      // In a real implementation:
      // JedisPool pool = new JedisPool(redis.getHost(), redis.getPort());
      // or use Spring Data Redis with RedisTemplate
      log.warn("Redis provider configured but not yet implemented - falling back to in-memory");
    } catch (Exception e) {
      log.error("Failed to initialize Redis, falling back to in-memory: {}", e.getMessage());
    }
  }

  // ===========================================
  // CACHE ENTRY DATA STRUCTURE
  // ===========================================

  @Data
  @Builder
  @AllArgsConstructor
  public static class CacheEntry<T> {
    private T data;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt; // null = infinite (never expires by time)
    private String dataHash; // For change detection
    private int hitCount; // Track cache hits for metrics

    /** Check if entry is expired. Returns false if expiresAt is null (infinite TTL). */
    public boolean isExpired() {
      if (expiresAt == null) {
        return false; // Infinite TTL - never expires by time
      }
      return LocalDateTime.now().isAfter(expiresAt);
    }

    public void incrementHitCount() {
      this.hitCount++;
    }
  }

  @Data
  @AllArgsConstructor
  public static class DataVersion {
    private String hash;
    private LocalDateTime updatedAt;
  }

  @Data
  @Builder
  public static class CacheStats {
    private int totalEntries;
    private int pitchRiskEntries;
    private int cycleRiskEntries;
    private int qaEntries;
    private long totalHits;
    private String provider;
    private boolean riskCacheEnabled;
    private boolean qaCacheEnabled;
  }

  // ===========================================
  // PITCH RISK CACHING
  // ===========================================

  /**
   * Get cached pitch risk analysis
   *
   * @param pitchId The pitch ID
   * @param withAI Whether this is AI-enhanced analysis
   * @return Optional containing cached data if valid cache exists
   */
  public Optional<PitchRiskDTO> getCachedPitchRisk(Long pitchId, boolean withAI) {
    if (!cacheConfig.isRiskCacheEnabled()) {
      return Optional.empty();
    }

    String cacheKey = buildPitchRiskCacheKey(pitchId, withAI);
    CacheEntry<PitchRiskDTO> entry = pitchRiskCache.get(cacheKey);

    if (entry == null) {
      log.debug("Cache miss: pitch risk {} (withAI={})", pitchId, withAI);
      return Optional.empty();
    }

    if (entry.isExpired()) {
      log.debug("Cache expired: pitch risk {} (withAI={})", pitchId, withAI);
      pitchRiskCache.remove(cacheKey);
      return Optional.empty();
    }

    // Check if data has changed (if change detection is enabled)
    if (cacheConfig.getRisk().isInvalidateOnDataChange()) {
      DataVersion currentVersion = pitchDataVersions.get(pitchId);
      if (currentVersion != null && !currentVersion.getHash().equals(entry.getDataHash())) {
        log.debug("Cache invalidated due to data change: pitch {}", pitchId);
        pitchRiskCache.remove(cacheKey);
        return Optional.empty();
      }
    }

    entry.incrementHitCount();
    log.debug(
        "Cache hit: pitch risk {} (withAI={}, hits={})", pitchId, withAI, entry.getHitCount());
    return Optional.of(entry.getData());
  }

  /** Cache pitch risk analysis result */
  public void cachePitchRisk(Long pitchId, boolean withAI, PitchRiskDTO riskData, String dataHash) {
    if (!cacheConfig.isRiskCacheEnabled()) {
      return;
    }

    String cacheKey = buildPitchRiskCacheKey(pitchId, withAI);
    int ttlMinutes = cacheConfig.getRisk().getPitchTtlMinutes();

    // TTL = 0 means infinite (null expiresAt)
    LocalDateTime expiresAt = ttlMinutes > 0 ? LocalDateTime.now().plusMinutes(ttlMinutes) : null;

    CacheEntry<PitchRiskDTO> entry =
        CacheEntry.<PitchRiskDTO>builder()
            .data(riskData)
            .createdAt(LocalDateTime.now())
            .expiresAt(expiresAt)
            .dataHash(dataHash)
            .hitCount(0)
            .build();

    pitchRiskCache.put(cacheKey, entry);

    // Store data version for change detection
    pitchDataVersions.put(pitchId, new DataVersion(dataHash, LocalDateTime.now()));

    log.debug(
        "Cached pitch risk {} (withAI={}, TTL={})",
        pitchId,
        withAI,
        ttlMinutes == 0 ? "infinite" : ttlMinutes + " min");
  }

  /** Invalidate pitch risk cache when data changes */
  public void invalidatePitchRiskCache(Long pitchId) {
    pitchRiskCache.remove(buildPitchRiskCacheKey(pitchId, true));
    pitchRiskCache.remove(buildPitchRiskCacheKey(pitchId, false));
    pitchDataVersions.remove(pitchId);
    log.debug("Invalidated cache for pitch {}", pitchId);
  }

  /** Update data version to trigger cache invalidation on next access */
  public void updatePitchDataVersion(Long pitchId, String newHash) {
    pitchDataVersions.put(pitchId, new DataVersion(newHash, LocalDateTime.now()));
    log.debug("Updated data version for pitch {}", pitchId);
  }

  private String buildPitchRiskCacheKey(Long pitchId, boolean withAI) {
    return String.format("pitch_risk_%d_%s", pitchId, withAI ? "ai" : "rule");
  }

  // ===========================================
  // CYCLE RISK CACHING
  // ===========================================

  /** Get cached cycle risk overview */
  public Optional<CycleRiskOverviewDTO> getCachedCycleRisk(Long cycleId, boolean withAI) {
    if (!cacheConfig.isRiskCacheEnabled()) {
      return Optional.empty();
    }

    String cacheKey = buildCycleRiskCacheKey(cycleId, withAI);
    CacheEntry<CycleRiskOverviewDTO> entry = cycleRiskCache.get(cacheKey);

    if (entry == null || entry.isExpired()) {
      if (entry != null) {
        cycleRiskCache.remove(cacheKey);
      }
      return Optional.empty();
    }

    entry.incrementHitCount();
    log.debug(
        "Cache hit: cycle risk {} (withAI={}, hits={})", cycleId, withAI, entry.getHitCount());
    return Optional.of(entry.getData());
  }

  /** Cache cycle risk overview result */
  public void cacheCycleRisk(Long cycleId, boolean withAI, CycleRiskOverviewDTO riskData) {
    if (!cacheConfig.isRiskCacheEnabled()) {
      return;
    }

    String cacheKey = buildCycleRiskCacheKey(cycleId, withAI);
    int ttlMinutes = cacheConfig.getRisk().getCycleTtlMinutes();

    // TTL = 0 means infinite (null expiresAt)
    LocalDateTime expiresAt = ttlMinutes > 0 ? LocalDateTime.now().plusMinutes(ttlMinutes) : null;

    CacheEntry<CycleRiskOverviewDTO> entry =
        CacheEntry.<CycleRiskOverviewDTO>builder()
            .data(riskData)
            .createdAt(LocalDateTime.now())
            .expiresAt(expiresAt)
            .hitCount(0)
            .build();

    cycleRiskCache.put(cacheKey, entry);
    log.debug(
        "Cached cycle risk {} (withAI={}, TTL={})",
        cycleId,
        withAI,
        ttlMinutes == 0 ? "infinite" : ttlMinutes + " min");
  }

  /** Invalidate cycle risk cache */
  public void invalidateCycleRiskCache(Long cycleId) {
    cycleRiskCache.remove(buildCycleRiskCacheKey(cycleId, true));
    cycleRiskCache.remove(buildCycleRiskCacheKey(cycleId, false));
    log.debug("Invalidated cache for cycle {}", cycleId);
  }

  private String buildCycleRiskCacheKey(Long cycleId, boolean withAI) {
    return String.format("cycle_risk_%d_%s", cycleId, withAI ? "ai" : "rule");
  }

  // ===========================================
  // Q&A CACHING WITH FUZZY MATCHING
  // ===========================================

  /**
   * Get cached Q&A response with fuzzy matching support
   *
   * @param question The question being asked
   * @param contextType Context type (pitch, cycle, team, etc.)
   * @param contextId Optional context ID
   * @param cycleId Optional cycle ID
   * @param teamId Optional team ID
   * @return Optional containing cached response if similar question was asked recently
   */
  public Optional<QAResponse> getCachedQAResponse(
      String question, String contextType, Long contextId, Long cycleId, Long teamId) {

    if (!cacheConfig.isQACacheEnabled()) {
      return Optional.empty();
    }

    String contextKey = buildQAContextKey(contextType, contextId, cycleId, teamId);
    String normalizedQuestion = normalizeQuestion(question);

    // Try exact match first
    String exactKey = buildQACacheKey(normalizedQuestion, contextKey);
    CacheEntry<QAResponse> exactEntry = qaCache.get(exactKey);

    if (exactEntry != null && !exactEntry.isExpired()) {
      exactEntry.incrementHitCount();
      log.debug("Q&A cache exact hit (hits={})", exactEntry.getHitCount());
      return Optional.of(exactEntry.getData());
    }

    // Try fuzzy matching
    double threshold = cacheConfig.getQa().getSimilarityThreshold();
    if (threshold < 1.0) {
      for (Map.Entry<String, CacheEntry<QAResponse>> entry : qaCache.entrySet()) {
        String key = entry.getKey();
        CacheEntry<QAResponse> cacheEntry = entry.getValue();

        // Only check entries in same context
        if (!key.endsWith(contextKey) || cacheEntry.isExpired()) {
          continue;
        }

        // Extract cached question from the response
        String cachedQuestion = cacheEntry.getData().getQuestion();
        if (cachedQuestion == null) continue;

        double similarity = calculateSimilarity(question, cachedQuestion);
        if (similarity >= threshold) {
          cacheEntry.incrementHitCount();
          log.debug(
              "Q&A cache fuzzy hit (similarity={}, threshold={}, hits={})",
              String.format("%.2f", similarity),
              threshold,
              cacheEntry.getHitCount());
          return Optional.of(cacheEntry.getData());
        }
      }
    }

    log.debug("Q&A cache miss for context: {}", contextKey);
    return Optional.empty();
  }

  /** Cache Q&A response */
  public void cacheQAResponse(
      String question,
      String contextType,
      Long contextId,
      Long cycleId,
      Long teamId,
      QAResponse response) {

    if (!cacheConfig.isQACacheEnabled()) {
      return;
    }

    String contextKey = buildQAContextKey(contextType, contextId, cycleId, teamId);
    String normalizedQuestion = normalizeQuestion(question);
    String cacheKey = buildQACacheKey(normalizedQuestion, contextKey);

    int ttlHours = cacheConfig.getQa().getTtlHours();

    // TTL = 0 means infinite (null expiresAt)
    LocalDateTime expiresAt = ttlHours > 0 ? LocalDateTime.now().plusHours(ttlHours) : null;

    CacheEntry<QAResponse> entry =
        CacheEntry.<QAResponse>builder()
            .data(response)
            .createdAt(LocalDateTime.now())
            .expiresAt(expiresAt)
            .hitCount(0)
            .build();

    qaCache.put(cacheKey, entry);

    // Enforce max entries per context
    enforceQAContextLimit(contextKey);

    log.debug(
        "Cached Q&A response (context={}, TTL={})",
        contextKey,
        ttlHours == 0 ? "infinite" : ttlHours + " hours");
  }

  /** Invalidate Q&A cache for a specific context */
  public void invalidateQACache(String contextType, Long contextId) {
    String contextKeyPart = contextType + "_" + (contextId != null ? contextId : "");
    qaCache.entrySet().removeIf(entry -> entry.getKey().contains(contextKeyPart));
    log.debug("Invalidated Q&A cache for context: {}_{}", contextType, contextId);
  }

  private String buildQAContextKey(String contextType, Long contextId, Long cycleId, Long teamId) {
    return String.format(
        "%s_%d_%d_%d",
        contextType != null ? contextType : "global",
        contextId != null ? contextId : 0,
        cycleId != null ? cycleId : 0,
        teamId != null ? teamId : 0);
  }

  private String buildQACacheKey(String normalizedQuestion, String contextKey) {
    return String.format("qa_%s_%s", hashString(normalizedQuestion), contextKey);
  }

  /** Normalize question for comparison */
  private String normalizeQuestion(String question) {
    if (question == null) return "";
    return question.toLowerCase().replaceAll("[^a-z0-9\\s]", "").replaceAll("\\s+", " ").trim();
  }

  /** Calculate Jaccard similarity between two questions */
  private double calculateSimilarity(String q1, String q2) {
    String norm1 = normalizeQuestion(q1);
    String norm2 = normalizeQuestion(q2);

    if (norm1.equals(norm2)) return 1.0;

    Set<String> words1 = new HashSet<>(Arrays.asList(norm1.split(" ")));
    Set<String> words2 = new HashSet<>(Arrays.asList(norm2.split(" ")));

    Set<String> intersection = new HashSet<>(words1);
    intersection.retainAll(words2);

    Set<String> union = new HashSet<>(words1);
    union.addAll(words2);

    if (union.isEmpty()) return 0.0;
    return (double) intersection.size() / union.size();
  }

  /** Simple string hash for cache keys */
  private String hashString(String str) {
    int hash = 0;
    for (int i = 0; i < str.length(); i++) {
      hash = ((hash << 5) - hash) + str.charAt(i);
      hash = hash & hash;
    }
    return Integer.toHexString(Math.abs(hash));
  }

  /** Enforce maximum entries per context to prevent memory bloat */
  private void enforceQAContextLimit(String contextKey) {
    int maxEntries = cacheConfig.getQa().getMaxEntriesPerContext();

    List<Map.Entry<String, CacheEntry<QAResponse>>> contextEntries =
        qaCache.entrySet().stream()
            .filter(e -> e.getKey().endsWith(contextKey))
            .sorted(Comparator.comparing(e -> e.getValue().getCreatedAt()))
            .collect(Collectors.toList());

    if (contextEntries.size() > maxEntries) {
      int toRemove = contextEntries.size() - maxEntries;
      for (int i = 0; i < toRemove; i++) {
        qaCache.remove(contextEntries.get(i).getKey());
      }
      log.debug("Evicted {} oldest Q&A entries for context: {}", toRemove, contextKey);
    }
  }

  // ===========================================
  // CACHE MAINTENANCE
  // ===========================================

  /** Scheduled cleanup of expired cache entries (every 5 minutes) */
  @Scheduled(fixedRate = 300000)
  public void cleanupExpiredEntries() {
    int pitchRemoved = 0, cycleRemoved = 0, qaRemoved = 0;

    for (Iterator<Map.Entry<String, CacheEntry<PitchRiskDTO>>> it =
            pitchRiskCache.entrySet().iterator();
        it.hasNext(); ) {
      if (it.next().getValue().isExpired()) {
        it.remove();
        pitchRemoved++;
      }
    }

    for (Iterator<Map.Entry<String, CacheEntry<CycleRiskOverviewDTO>>> it =
            cycleRiskCache.entrySet().iterator();
        it.hasNext(); ) {
      if (it.next().getValue().isExpired()) {
        it.remove();
        cycleRemoved++;
      }
    }

    for (Iterator<Map.Entry<String, CacheEntry<QAResponse>>> it = qaCache.entrySet().iterator();
        it.hasNext(); ) {
      if (it.next().getValue().isExpired()) {
        it.remove();
        qaRemoved++;
      }
    }

    if (pitchRemoved + cycleRemoved + qaRemoved > 0) {
      log.info(
          "Cache cleanup: removed {} pitch, {} cycle, {} Q&A expired entries",
          pitchRemoved,
          cycleRemoved,
          qaRemoved);
    }
  }

  /** Clear all caches */
  public void clearAllCaches() {
    pitchRiskCache.clear();
    cycleRiskCache.clear();
    qaCache.clear();
    pitchDataVersions.clear();
    log.info("All AI caches cleared");
  }

  /** Get cache statistics */
  public CacheStats getCacheStats() {
    long totalHits =
        pitchRiskCache.values().stream().mapToInt(CacheEntry::getHitCount).sum()
            + cycleRiskCache.values().stream().mapToInt(CacheEntry::getHitCount).sum()
            + qaCache.values().stream().mapToInt(CacheEntry::getHitCount).sum();

    return CacheStats.builder()
        .totalEntries(pitchRiskCache.size() + cycleRiskCache.size() + qaCache.size())
        .pitchRiskEntries(pitchRiskCache.size())
        .cycleRiskEntries(cycleRiskCache.size())
        .qaEntries(qaCache.size())
        .totalHits(totalHits)
        .provider(cacheConfig.getProvider())
        .riskCacheEnabled(cacheConfig.isRiskCacheEnabled())
        .qaCacheEnabled(cacheConfig.isQACacheEnabled())
        .build();
  }
}
