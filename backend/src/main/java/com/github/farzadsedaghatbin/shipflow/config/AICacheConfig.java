package com.github.farzadsedaghatbin.shipflow.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for AI response caching with feature flags.
 * Allows fine-grained control over caching behavior for different AI features.
 * 
 * Supports two cache providers:
 * - in-memory: Simple ConcurrentHashMap, data lost on restart (good for dev)
 * - redis: Persistent cache across restarts (recommended for production)
 */
@Configuration
@ConfigurationProperties(prefix = "app.ai.cache")
@ConditionalOnProperty(name = "app.ai.risk-analysis.enabled", havingValue = "true", matchIfMissing = false)
@Getter
@Setter
@Slf4j
public class AICacheConfig {

    /**
     * Master switch to enable/disable all AI caching
     */
    private boolean enabled = true;

    /**
     * Cache provider: "in-memory" or "redis"
     */
    private String provider = "in-memory";

    /**
     * Redis configuration (only used when provider=redis)
     */
    private RedisConfig redis = new RedisConfig();

    /**
     * Risk analysis cache configuration
     */
    private RiskCacheConfig risk = new RiskCacheConfig();

    /**
     * Q&A response cache configuration
     */
    private QACacheConfig qa = new QACacheConfig();

    @Getter
    @Setter
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private String password = "";
        private int database = 0;
    }

    @Getter
    @Setter
    public static class RiskCacheConfig {
        /**
         * Enable caching for risk analysis responses
         */
        private boolean enabled = true;

        /**
         * Cache TTL in minutes for pitch risk analysis.
         * 0 = infinite (until data changes or server restart)
         * Since we have automatic invalidation on data changes, 0 is recommended.
         */
        private int pitchTtlMinutes = 0;

        /**
         * Cache TTL in minutes for cycle risk overview.
         * 0 = infinite (until data changes or server restart)
         */
        private int cycleTtlMinutes = 0;

        /**
         * Invalidate cache when pitch data changes (work logs, status, etc.)
         */
        private boolean invalidateOnDataChange = true;
    }

    @Getter
    @Setter
    public static class QACacheConfig {
        /**
         * Enable caching for Q&A responses
         */
        private boolean enabled = true;

        /**
         * Cache TTL in hours for Q&A responses.
         * 0 = infinite, >0 = hours
         * For Q&A, some TTL is recommended (2-3 hours) as knowledge base may be updated.
         */
        private int ttlHours = 3;

        /**
         * Similarity threshold (0.0-1.0) for matching similar questions
         * Higher values require more exact matches
         */
        private double similarityThreshold = 0.85;

        /**
         * Maximum cached Q&A entries per context (to prevent memory bloat)
         */
        private int maxEntriesPerContext = 100;
    }

    /**
     * Log current configuration on startup
     */
    public void logConfiguration() {
        log.info("AI Cache Configuration:");
        log.info("  Master enabled: {}", enabled);
        log.info("  Provider: {}", provider);
        if ("redis".equalsIgnoreCase(provider)) {
            log.info("  Redis: {}:{}/{}", redis.host, redis.port, redis.database);
        }
        log.info("  Risk Analysis Cache:");
        log.info("    - Enabled: {}", risk.enabled);
        log.info("    - Pitch TTL: {} (0=infinite)", risk.pitchTtlMinutes == 0 ? "infinite" : risk.pitchTtlMinutes + " minutes");
        log.info("    - Cycle TTL: {} (0=infinite)", risk.cycleTtlMinutes == 0 ? "infinite" : risk.cycleTtlMinutes + " minutes");
        log.info("    - Invalidate on data change: {}", risk.invalidateOnDataChange);
        log.info("  Q&A Cache:");
        log.info("    - Enabled: {}", qa.enabled);
        log.info("    - TTL: {} (0=infinite)", qa.ttlHours == 0 ? "infinite" : qa.ttlHours + " hours");
        log.info("    - Similarity threshold: {}", qa.similarityThreshold);
        log.info("    - Max entries per context: {}", qa.maxEntriesPerContext);
    }

    /**
     * Check if risk cache is effectively enabled
     */
    public boolean isRiskCacheEnabled() {
        return enabled && risk.enabled;
    }

    /**
     * Check if Q&A cache is effectively enabled
     */
    public boolean isQACacheEnabled() {
        return enabled && qa.enabled;
    }

    /**
     * Check if using Redis provider
     */
    public boolean isRedisProvider() {
        return "redis".equalsIgnoreCase(provider);
    }
}
