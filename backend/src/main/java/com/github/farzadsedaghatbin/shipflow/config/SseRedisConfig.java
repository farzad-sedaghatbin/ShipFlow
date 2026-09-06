package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.service.NotificationSseManager;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

/**
 * Wires Redis pub/sub so SSE notifications published by {@link NotificationSseManager} on one
 * backend pod are relayed to every other pod's locally-connected emitters. Only active when Redis
 * caching is enabled ({@code spring.cache.type=redis}) — matches {@code RedisConfig}'s gate, so
 * this is a no-op wiring under the test profile (spring.cache.type=simple).
 */
@Configuration
@ConditionalOnProperty(name = "spring.cache.type", havingValue = "redis")
public class SseRedisConfig {

  @Bean
  public ChannelTopic sseFanOutTopic() {
    return new ChannelTopic(NotificationSseManager.FANOUT_CHANNEL);
  }

  @Bean
  public RedisMessageListenerContainer sseFanOutListenerContainer(
      RedisConnectionFactory connectionFactory,
      NotificationSseManager notificationSseManager,
      ChannelTopic sseFanOutTopic) {
    RedisMessageListenerContainer container = new RedisMessageListenerContainer();
    container.setConnectionFactory(connectionFactory);
    container.addMessageListener(
        (message, pattern) -> notificationSseManager.onRemoteMessage(message.getBody()),
        sseFanOutTopic);
    return container;
  }
}
