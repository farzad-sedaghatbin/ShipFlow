package com.github.farzadsedaghatbin.shipflow.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metrics configuration for RAG/QA monitoring.
 */
@Configuration
@ConditionalOnClass(MeterRegistry.class)
@Slf4j
public class RAGMetricsConfig {
    
    /**
     * Timer for Q&A query processing.
     */
    @Bean
    public Timer qaQueryTimer(MeterRegistry registry) {
        return Timer.builder("qa.query.duration")
                .description("Time taken to process Q&A queries")
                .tag("service", "qa")
                .register(registry);
    }
    
    /**
     * Counter for Q&A queries.
     */
    @Bean
    public Counter qaQueryCounter(MeterRegistry registry) {
        return Counter.builder("qa.query.total")
                .description("Total number of Q&A queries processed")
                .tag("service", "qa")
                .register(registry);
    }
    
    /**
     * Counter for cache hits.
     */
    @Bean
    public Counter qaCacheHitCounter(MeterRegistry registry) {
        return Counter.builder("qa.cache.hits")
                .description("Number of Q&A cache hits")
                .tag("service", "qa")
                .register(registry);
    }
    
    /**
     * Counter for retrieval operations.
     */
    @Bean
    public Counter qaRetrievalCounter(MeterRegistry registry) {
        return Counter.builder("qa.retrieval.total")
                .description("Number of document retrieval operations")
                .tag("service", "qa")
                .register(registry);
    }
    
    /**
     * Timer for LLM generation.
     */
    @Bean
    public Timer qaLlmTimer(MeterRegistry registry) {
        return Timer.builder("qa.llm.duration")
                .description("Time taken for LLM to generate answers")
                .tag("service", "qa")
                .register(registry);
    }
    
    /**
     * Counter for LLM errors.
     */
    @Bean
    public Counter qaLlmErrorCounter(MeterRegistry registry) {
        return Counter.builder("qa.llm.errors")
                .description("Number of LLM generation errors")
                .tag("service", "qa")
                .register(registry);
    }
    
    /**
     * Counter for test case generation.
     */
    @Bean
    public Counter testGenerationCounter(MeterRegistry registry) {
        return Counter.builder("qa.test.generation.total")
                .description("Number of test case generation requests")
                .tag("service", "test-generation")
                .register(registry);
    }
    
    /**
     * Counter for validation failures.
     */
    @Bean
    public Counter testValidationFailureCounter(MeterRegistry registry) {
        return Counter.builder("qa.test.validation.failures")
                .description("Number of test case validation failures")
                .tag("service", "test-generation")
                .register(registry);
    }
}
