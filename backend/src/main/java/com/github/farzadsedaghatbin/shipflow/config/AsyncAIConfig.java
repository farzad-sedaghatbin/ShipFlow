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
   * - Core pool: 2 threads (for normal load)
   * - Max pool: 5 threads (for burst AI requests)
   * - Queue capacity: 50 (buffer for pending requests)
   * - Thread name prefix for easy identification in logs
   */
  @Bean(name = "aiTaskExecutor")
  public Executor aiTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(50);
    executor.setThreadNamePrefix("AI-Advisor-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(30);
    executor.initialize();
    log.info("AI Task Executor initialized with core={}, max={}, queue={}",
        executor.getCorePoolSize(), executor.getMaxPoolSize(), executor.getQueueCapacity());
    return executor;
  }
}
