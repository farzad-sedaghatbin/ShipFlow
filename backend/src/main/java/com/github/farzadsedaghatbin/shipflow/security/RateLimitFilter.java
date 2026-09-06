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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

  private final ClientIpService clientIpService;

  public RateLimitFilter(ClientIpService clientIpService) {
    this.clientIpService = clientIpService;
  }

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

  @Value("${app.rate-limit.csv-import.capacity:5}")
  private int csvImportCapacity;

  @Value("${app.rate-limit.linear-import.capacity:3}")
  private int linearImportCapacity;

  @Value("${app.rate-limit.jira-import.capacity:3}")
  private int jiraImportCapacity;

  @Value("${app.rate-limit.public-api.capacity:100}")
  private int publicApiCapacity;

  @Value("${app.rate-limit.trusted-proxies:127.0.0.1,::1}")
  private String trustedProxiesRaw;

  private List<String> trustedProxies;

  private static final Duration AUTH_PERIOD = Duration.ofMinutes(1);
  private static final Duration SEARCH_PERIOD = Duration.ofMinutes(1);
  private static final Duration AI_PERIOD = Duration.ofMinutes(1);
  private static final Duration RISK_READ_PERIOD = Duration.ofMinutes(1);
  private static final Duration POLL_PERIOD = Duration.ofMinutes(1);
  private static final Duration CSV_IMPORT_PERIOD = Duration.ofMinutes(1);
  private static final Duration LINEAR_IMPORT_PERIOD = Duration.ofMinutes(1);
  private static final Duration JIRA_IMPORT_PERIOD = Duration.ofMinutes(1);
  private static final Duration PUBLIC_API_PERIOD = Duration.ofMinutes(1);

  private static final int MAX_BUCKETS = 10_000;


  private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

  private static final String AUTH_PREFIX = "/api/auth/";
  private static final String SEARCH_PREFIX = "/api/search";
  private static final String WISE_ARCH_PREFIX = "/api/wise-architecture";
  private static final String RISK_PREFIX = "/api/risk";
  private static final String ASYNC_JOBS_PREFIX = "/api/risk/async/jobs/";
  private static final String CSV_IMPORT_PREFIX = "/api/import/";
  private static final String LINEAR_IMPORT_PREFIX = "/api/linear-import/";
  private static final String JIRA_IMPORT_PREFIX = "/api/jira-import/";
  // Every /api/v1/public/** call is auth'd by a custom X-API-Key filter, not by Spring
  // Security's own permitAll gate (see SecurityConfig) — so, unlike /api/auth/ or /api/search,
  // it previously had NO rate limit at all: unlimited full-project exports and unlimited
  // X-API-Key brute-force attempts (401s aren't throttled either, since this filter runs before
  // ApiKeyAuthenticationFilter can even reject an invalid key). Covers DataManagementController
  // too (mounted under /api/v1/public/data/**).
  private static final String PUBLIC_API_PREFIX = "/api/v1/public/";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String path = request.getRequestURI();
    RateLimit limit = resolveLimit(path, request.getMethod());

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
    if (path.startsWith(PUBLIC_API_PREFIX)) {
      // A public integration API (CI pipelines, GitHub Actions, external tooling) needs a more
      // generous budget than /api/auth/'s login-brute-force-sensitive limit, but must not be
      // unlimited — hence its own bucket rather than reusing "search" or "poll". Applies to
      // every method (GET list/export calls and the PATCH/POST write calls alike) since none of
      // the resources under this prefix are more sensitive than the others from a rate-limiting
      // standpoint; per-endpoint tiering can be added later if a specific one turns out to need
      // stricter limits.
      //
      // Keying: like every other bucket in this filter, this keys purely on client IP
      // (resolveClientIp/ClientIpResolver), not on the calling API key. Keying per-key would
      // require this filter to parse and validate X-API-Key itself (it currently runs before
      // ApiKeyAuthenticationFilter and knows nothing about API keys), which changes this
      // filter's responsibility and ordering — out of scope for this contained hardening pass.
      // IP-based limiting still bounds unlimited key brute-forcing and unauthenticated abuse
      // from a single source; a shared-IP scenario (e.g. many integrations behind one NAT/proxy)
      // sharing one bucket is an accepted tradeoff of that simplicity.
      return new RateLimit("public-api", publicApiCapacity, PUBLIC_API_PERIOD);
    }
    return null;
  }

  private RateLimit resolveLimit(String path, String method) {
    if (path == null) return null;
    // Apply stricter rate limits on POST requests to import endpoints
    if ("POST".equalsIgnoreCase(method)) {
      if (path.startsWith(CSV_IMPORT_PREFIX)) {
        return new RateLimit("csv-import", csvImportCapacity, CSV_IMPORT_PERIOD);
      }
      if (path.startsWith(LINEAR_IMPORT_PREFIX)) {
        return new RateLimit("linear-import", linearImportCapacity, LINEAR_IMPORT_PERIOD);
      }
      if (path.startsWith(JIRA_IMPORT_PREFIX)) {
        return new RateLimit("jira-import", jiraImportCapacity, JIRA_IMPORT_PERIOD);
      }
    }
    return resolveLimit(path);
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
    return clientIpService.resolve(request);
  }

  private record RateLimit(String group, int capacity, Duration period) {}
}
