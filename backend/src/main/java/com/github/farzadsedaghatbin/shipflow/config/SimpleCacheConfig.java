package com.github.farzadsedaghatbin.shipflow.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Fallback cache configuration when {@code spring.cache.type=simple}.
 *
 * <p>Uses a {@link ConcurrentMapCacheManager} backed by {@code ConcurrentHashMap} —
 * no external dependencies required. This is used in tests and local development
 * when Redis is unavailable.
 *
 * <p>The same {@code @Cacheable} / {@code @CacheEvict} annotations work regardless
 * of which cache manager is active (Redis or Simple).
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "simple")
public class SimpleCacheConfig {

    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager(
                "permissions",
                "projects",
                "cycles",
                "teams",
                "tags",
                "persons",
                "users",
                "roadmap",
                "risks",
                "qa");
    }
}
