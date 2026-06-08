package com.github.farzadsedaghatbin.shipflow.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.autoconfigure.cache.CacheProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis configuration.
 *
 * <p>Provides:
 * <ul>
 *   <li>{@link StringRedisTemplate} – for services that manage their own JSON serialization
 *   <li>{@link RedisTemplate} with Jackson – for generic use
 *   <li>{@link RedisCacheManager} – backs Spring {@code @Cacheable} annotations
 * </ul>
 *
 * <p>Enabled only when {@code spring.cache.type=redis} (i.e. dev and prod profiles with docker-compose).
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
@EnableConfigurationProperties(CacheProperties.class)
public class RedisConfig {

    /**
     * Creates a dedicated ObjectMapper for Redis value serialization.
     * This is intentionally NOT exposed as a Spring bean to avoid interfering
     * with Spring Boot's auto-configured primary ObjectMapper (used by MVC/REST).
     * Exposes an ObjectMapper bean in the context would suppress
     * JacksonAutoConfiguration's @ConditionalOnMissingBean(ObjectMapper.class),
     * causing the typed Redis mapper to be used for @RequestBody deserialization.
     */
    private ObjectMapper createRedisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // Embed type info so deserialization can reconstruct the correct class.
        // Restrict allowed types to mitigate deserialization gadget-chain attacks (CWE-502).
        //
        // IMPORTANT: must use allowIfSubType (checks the concrete resolved type) rather than
        // allowIfBaseType (checks the statically declared type). Spring Cache wraps return values
        // as Object, so the base type is always Object — allowIfBaseType(Collection.class) would
        // never match and Jackson throws InvalidTypeIdException for java.util.ArrayList etc.
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        // Our own DTOs
                        .allowIfSubType("com.github.farzadsedaghatbin.shipflow")
                        // Standard JDK collections / maps (ArrayList, HashMap, LinkedHashMap …)
                        .allowIfSubType("java.util.")
                        // java.time types (LocalDate, Instant, ZoneId …)
                        .allowIfSubType("java.time.")
                        .build(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        return mapper;
    }

    /**
     * Generic {@link RedisTemplate} with JSON-serialized values.
     * Keys are plain strings; values are JSON with embedded type info.
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());

        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        template.afterPropertiesSet();
        return template;
    }

    /**
     * {@link StringRedisTemplate} – both keys and values are plain strings.
     * Services that manage their own ObjectMapper serialization use this.
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * {@link RedisCacheManager} backing Spring's {@code @Cacheable} support.
     * TTL and key prefix are read from {@code spring.cache.redis.*} in application.properties.
     * Per-cache TTLs are configured here to match the expected rate of change for each domain.
     */
    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory, CacheProperties cacheProperties) {

        GenericJackson2JsonRedisSerializer jsonSerializer =
                new GenericJackson2JsonRedisSerializer(createRedisObjectMapper());

        CacheProperties.Redis redisProps = cacheProperties.getRedis();
        Duration ttl = redisProps.getTimeToLive() != null ? redisProps.getTimeToLive() : Duration.ofHours(1);
        String keyPrefix = redisProps.getKeyPrefix() != null ? redisProps.getKeyPrefix() : "shipflow:";

        RedisCacheConfiguration defaultConfig =
                RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(ttl)
                        .prefixCacheNameWith(keyPrefix)
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(
                                        new StringRedisSerializer()))
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
                        .disableCachingNullValues();

        // Per-cache TTL configurations — tuned to each domain's rate of change
        Map<String, RedisCacheConfiguration> perCacheConfig = new HashMap<>();
        perCacheConfig.put("permissions", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        perCacheConfig.put("projects", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        perCacheConfig.put("cycles", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        perCacheConfig.put("teams", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        perCacheConfig.put("tags", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        perCacheConfig.put("persons", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        perCacheConfig.put("users", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        perCacheConfig.put("roadmap", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        perCacheConfig.put("dashboardInsights", defaultConfig.entryTtl(Duration.ofMinutes(60)));
        perCacheConfig.put("projectSnapshot", defaultConfig.entryTtl(Duration.ofMinutes(90)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(perCacheConfig)
                .build();
    }
}
