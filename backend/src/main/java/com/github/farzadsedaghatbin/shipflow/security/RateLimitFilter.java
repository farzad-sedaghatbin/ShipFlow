package com.github.farzadsedaghatbin.shipflow.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.Refill;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  @Value("${app.rate-limit.auth.capacity:10}")
  private int authCapacity;

  @Value("${app.rate-limit.ai.capacity:20}")
  private int aiCapacity;

  @Value("${app.rate-limit.risk-read.capacity:60}")
  private int riskReadCapacity;

  @Value("${app.rate-limit.search.capacity:30}")
  private int searchCapacity;

  @Value("${app.rate-limit.poll.capacity:120}")
  private int pollCapacity;

  private static final Duration AUTH_PERIOD = Duration.ofMinutes(1);
  private static final Duration SEARCH_PERIOD = Duration.ofMinutes(1);
  private static final Duration AI_PERIOD = Duration.ofMinutes(1);
  private static final Duration RISK_READ_PERIOD = Duration.ofMinutes(1);
  private static final Duration POLL_PERIOD = Duration.ofMinutes(1);

  private static final int MAX_BUCKETS = 10_000;

  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  private static final String AUTH_PREFIX = "/api/auth/";
  private static final String SEARCH_PREFIX = "/api/search";
  private static final String WISE_ARCH_PREFIX = "/api/wise-architecture";
  private static final String RISK_PREFIX = "/api/risk";
  private static final String ASYNC_JOBS_PREFIX = "/api/risk/async/jobs/";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    RateLimit limit = resolveLimit(path);

    if (limit == null) {
      filterChain.doFilter(request, response);
      return;
    }

    String clientIp = resolveClientIp(request);
    String bucketKey = clientIp + ":" + limit.group;

    if (buckets.size() > MAX_BUCKETS) {
      buckets.clear();
      log.warn("Rate limit bucket cache exceeded {} entries, cleared", MAX_BUCKETS);
    }

    Bucket bucket = buckets.computeIfAbsent(bucketKey, k -> newBucket(limit));

    ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
    if (probe.isConsumed()) {
      filterChain.doFilter(request, response);
    } else {
      long retryAfterSeconds = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
      log.warn("Rate limit exceeded: ip={} path={} group={}", clientIp, path, limit.group);
      sendTooManyRequests(response, retryAfterSeconds);
    }
  }

  private RateLimit resolveLimit(String path) {
    if (path == null) return null;
    if (path.startsWith(AUTH_PREFIX)) {
      return new RateLimit("auth", authCapacity, AUTH_PERIOD);
    }
    if (path.startsWith(SEARCH_PREFIX)) {
      return new RateLimit("search", searchCapacity, SEARCH_PERIOD);
    }
    if (path.startsWith(ASYNC_JOBS_PREFIX)) {
      return new RateLimit("poll", pollCapacity, POLL_PERIOD);
    }
    if (path.startsWith(WISE_ARCH_PREFIX)) {
      return new RateLimit("ai", aiCapacity, AI_PERIOD);
    }
    if (path.startsWith(RISK_PREFIX)) {
      if (isAiTriggerPath(path)) {
        return new RateLimit("ai", aiCapacity, AI_PERIOD);
      }
      return new RateLimit("risk-read", riskReadCapacity, RISK_READ_PERIOD);
    }
    return null;
  }

  private boolean isAiTriggerPath(String path) {
    return path.contains("/analyze") || path.contains("/ask") || path.contains("/refresh");
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

  private String resolveClientIp(HttpServletRequest request) {
    String xff = request.getHeader("X-Forwarded-For");
    if (xff != null && !xff.isBlank()) {
      int commaIdx = xff.indexOf(',');
      return commaIdx >= 0 ? xff.substring(0, commaIdx).trim() : xff.trim();
    }
    String realIp = request.getHeader("X-Real-IP");
    if (realIp != null && !realIp.isBlank()) {
      return realIp.trim();
    }
    return request.getRemoteAddr();
  }

  private record RateLimit(String group, int capacity, Duration period) {}
}
