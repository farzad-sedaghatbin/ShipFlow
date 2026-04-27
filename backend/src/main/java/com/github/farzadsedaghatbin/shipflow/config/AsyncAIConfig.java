package com.github.farzadsedaghatbin.shipflow.config;

import java.util.concurrent.Executor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Async configuration for AI operations.
 * Provides a dedicated thread pool for AI advisory calls to prevent
 * blocking the main request threads when AI responses are slow or not cached.
 */
@Configuration
@EnableAsync
@Slf4j
public class AsyncAIConfig {

  /**
   * Thread pool executor for AI operations.
   * - Core pool: 10 threads (handles concurrent AI requests without queueing)
   * - Max pool: 20 threads (for burst AI requests)
   * - Queue capacity: 100 (buffer for pending requests when pool is full)
   * - CallerRunsPolicy: If queue is full, caller thread executes the task (backpressure)
   * - Thread name prefix for easy identification in logs
   * 
   * Sizing rationale:
   * - OpenAI has rate limits (~60 RPM for gpt-4-turbo), so 10-20 concurrent is reasonable
   * - Each AI call blocks 5-120s, so more threads = more concurrent capacity
   * - CallerRunsPolicy provides natural backpressure under extreme load
   */
  @Bean(name = "aiTaskExecutor")
  public Executor aiTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(10);
    executor.setMaxPoolSize(20);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("AI-Advisor-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    // CallerRunsPolicy: When queue is full, the calling thread runs the task
    // This provides natural backpressure and prevents task rejection
    executor.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
    executor.initialize();
    log.info("AI Task Executor initialized with core={}, max={}, queue={}, rejectionPolicy=CallerRunsPolicy",
        executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
    return executor;
  }
}
