package com.github.farzadsedaghatbin.shipflow.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/** Configuration for HTTP clients including RestTemplate */
@Configuration
public class HttpClientConfig {

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.setConnectTimeout(Duration.ofSeconds(15)).setReadTimeout(Duration.ofSeconds(60))
        // Add error handler to prevent connection reset issues
        .additionalMessageConverters(new org.springframework.http.converter.StringHttpMessageConverter())
        .build();
  }

  /**
   * Dedicated RestTemplate for webhook calls with specific timeout and error
   * handling
   */
  @Bean("webhookRestTemplate")
  public RestTemplate webhookRestTemplate(RestTemplateBuilder builder) {
    return builder.setConnectTimeout(Duration.ofSeconds(30)).setReadTimeout(Duration.ofSeconds(90))
        // More aggressive timeout for webhooks
        .additionalMessageConverters(new org.springframework.http.converter.StringHttpMessageConverter())
        .build();
  }

  /**
   * Dedicated RestTemplate for MCP (Model Context Protocol) server calls.
   * Has longer timeouts as MCP operations may involve fetching external data.
   * Includes custom SSE (Server-Sent Events) message converter for handling text/event-stream responses.
   * 
   * Note: Increased timeout from 30s to 120s to handle large repository operations.
   * For very large repos (1000+ files), use the async endpoints instead.
   */
  @Bean("mcpRestTemplate")
  public RestTemplate mcpRestTemplate(RestTemplateBuilder builder, ObjectMapper objectMapper) {
    return builder.setConnectTimeout(Duration.ofSeconds(10)).setReadTimeout(Duration.ofSeconds(120))
        .additionalMessageConverters(
            new SseJsonMessageConverter(objectMapper),
            new org.springframework.http.converter.StringHttpMessageConverter())
        .build();
  }
}
