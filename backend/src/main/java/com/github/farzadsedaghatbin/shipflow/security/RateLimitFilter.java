package com.github.farzadsedaghatbin.shipflow.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate-limiting filter that enforces per-IP request limits on sensitive API
 * paths using Bucket4j (in-memory token-bucket algorithm).
 *
 * <p>Limits:
 * <ul>
 *   <li>{@code /api/auth/**} — 10 requests / minute</li>
 *   <li>{@code /api/search/**} — 30 requests / minute</li>
 *   <li>{@code /api/wise-architecture/**} — 5 requests / minute</li>
 *   <li>{@code /api/risk/**} — 5 requests / minute</li>
 * </ul>
 *
 * <p>Requests that exceed the limit receive HTTP 429 with a JSON body and a
 * {@code Retry-After} header indicating how many seconds to wait.
 */
@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  // ---- Configuration constants ----

  /**
   * Auth endpoints: configurable requests per minute per IP.
   * Default 10 (production-safe). Override in dev/test profiles via
   * {@code app.rate-limit.auth.capacity} to avoid blocking E2E test suites
   * that re-login many times from the same runner IP.
   */
  @Value("${app.rate-limit.auth.capacity:10}")
  private int authCapacity;

  private static final Duration AUTH_PERIOD = Duration.ofMinutes(1);
  private static final long AUTH_RETRY_AFTER_SECONDS = 60;

  /** Search endpoint: 30 requests per minute per IP. */
  private static final int SEARCH_CAPACITY = 30;
  private static final Duration SEARCH_PERIOD = Duration.ofMinutes(1);
  private static final long SEARCH_RETRY_AFTER_SECONDS = 60;

  /** AI endpoints (wise-architecture, risk): 5 requests per minute per IP. */
  private static final int AI_CAPACITY = 5;
  private static final Duration AI_PERIOD = Duration.ofMinutes(1);
  private static final long AI_RETRY_AFTER_SECONDS = 60;

  // ---- Bucket stores (one ConcurrentHashMap per rate-limited path group) ----

  /** Key: {@code "<ip>:<path-group>"} → Bucket */
  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  // ---- Path prefixes for each limit group ----
  private static final String AUTH_PREFIX = "/api/auth/";
  private static final String SEARCH_PREFIX = "/api/search";
  private static final String WISE_ARCH_PREFIX = "/api/wise-architecture";
  private static final String RISK_PREFIX = "/api/risk";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    RateLimit limit = resolveLimit(path);

    if (limit == null) {
      // Path is not rate-limited — pass through immediately
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = resolveClientIp(request);
    String bucketKey = clientIp + ":" + limit.group;
    Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> newBucket(limit));

    if (bucket.tryConsume(1)) {
      filterChain.doFilter(request, response);
    } else {
      log.warn("Rate limit exceeded: ip={} path={} group={}", clientIp, path, limit.group);
      sendTooManyRequests(response, limit.retryAfterSeconds);
    }
  }

  // ---- Helpers ----

  private RateLimit resolveLimit(String path) {
    if (path == null) return null;
    if (path.startsWith(AUTH_PREFIX)) {
      return new RateLimit("auth", authCapacity, AUTH_PERIOD, AUTH_RETRY_AFTER_SECONDS);
    }
    if (path.startsWith(SEARCH_PREFIX)) {
      return new RateLimit("search", SEARCH_CAPACITY, SEARCH_PERIOD, SEARCH_RETRY_AFTER_SECONDS);
    }
    if (path.startsWith(WISE_ARCH_PREFIX) || path.startsWith(RISK_PREFIX)) {
      return new RateLimit("ai", AI_CAPACITY, AI_PERIOD, AI_RETRY_AFTER_SECONDS);
    }
    return null;
  }

  private Bucket newBucket(RateLimit limit) {
    Bandwidth bandwidth =
        Bandwidth.classic(limit.capacity, Refill.greedy(limit.capacity, limit.period));
    return Bucket.builder().addLimit(bandwidth).build();
  }

  private void sendTooManyRequests(HttpServletResponse response, long retryAfterSeconds)
      throws IOException {
    response.setStatus(429);
    response.setContentType("application/json");
    response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
    response
        .getWriter()
        .write(
            "{\"error\":\"Too many requests\",\"retryAfter\":"
                + retryAfterSeconds
                + "}");
  }

  /**
   * Resolves the real client IP, preferring the first value in
   * {@code X-Forwarded-For} when present (set by the load balancer /
   * reverse proxy).
   */
  private String resolveClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      // X-Forwarded-For may be a comma-separated list; take the first entry
      int commaIdx = xff.indexOf(',');
      return commaIdx >= 0 ? xff.substring(0, commaIdx).trim() : xff.trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    return request.getRemoteAddr();
  }

  // ---- Value type ----

  private record RateLimit(String group, int capacity, Duration period, long retryAfterSeconds) {}
}
