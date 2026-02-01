package com.github.farzadsedaghatbin.shipflow.config;

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
    return builder
        .setConnectTimeout(Duration.ofSeconds(15))
        .setReadTimeout(Duration.ofSeconds(60))
        // Add error handler to prevent connection reset issues
        .additionalMessageConverters(
            new org.springframework.http.converter.StringHttpMessageConverter())
        .build();
  }

  /** Dedicated RestTemplate for webhook calls with specific timeout and error handling */
  @Bean("webhookRestTemplate")
  public RestTemplate webhookRestTemplate(RestTemplateBuilder builder) {
    return builder
        .setConnectTimeout(Duration.ofSeconds(30))
        .setReadTimeout(Duration.ofSeconds(90))
        // More aggressive timeout for webhooks
        .additionalMessageConverters(
            new org.springframework.http.converter.StringHttpMessageConverter())
        .build();
  }
}
