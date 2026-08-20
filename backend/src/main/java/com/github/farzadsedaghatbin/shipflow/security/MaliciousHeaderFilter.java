package com.github.farzadsedaghatbin.shipflow.security;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Security filter to detect and block malicious requests containing exploit
 * patterns such as Log4Shell (CVE-2021-44228), JNDI injection, GPON router
 * exploits (CVE-2018-10561/10562), and other common attack vectors.
 */
@Slf4j
@Component
public class MaliciousHeaderFilter implements Filter {

  /**
   * Peers whose forwarding headers may be trusted. Defaults to loopback, which
   * is where Caddy connects from; must match the rate limiter's list or the two
   * filters would disagree about who a request is from.
   */
  @Value("${app.rate-limit.trusted-proxies:127.0.0.1,::1}")
  private String trustedProxiesRaw;

  private List<String> trustedProxies;

  @PostConstruct
  void initTrustedProxies() {
    trustedProxies =
        Arrays.stream(trustedProxiesRaw.split(","))
            .map(String::trim)
            .filter(entry -> !entry.isEmpty())
            .toList();
    log.info("MaliciousHeaderFilter trusted proxies: {}", trustedProxies);
  }

  // Patterns for detecting common exploit attempts
  private static final Pattern JNDI_PATTERN = Pattern.compile(
      ".*\\$\\{.*jndi:.*}.*|.*\\$\\{.*env:.*}.*|.*\\$\\{.*lower:.*}.*|.*\\$\\{.*upper:.*}.*",
      Pattern.CASE_INSENSITIVE);

  private static final Pattern SCRIPT_INJECTION_PATTERN = Pattern
      .compile(".*<script.*>.*</script>.*|.*javascript:.*|.*onerror=.*|.*onload=.*", Pattern.CASE_INSENSITIVE);

  private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(".*(\\.\\./)|(\\.\\.\\\\).*",
      Pattern.CASE_INSENSITIVE);

  // Headers that commonly contain malicious payloads
  private static final String[] SENSITIVE_HEADERS = {"X-Forwarded-Host", "X-Forwarded-For", "X-Forwarded-Proto",
      "Forwarded", "Host", "X-Real-IP", "User-Agent", "Referer"};

  /**
   * Known exploit/scanner URI path prefixes. Requests whose path starts with any
   * of these are blocked immediately with 403 to stop the noisy 405 errors that
   * arise when bots POST to GET-only resource paths and to prevent any accidental
   * handler resolution of exploit routes.
   *
   * <p>Covered CVEs / attack families:
   * <ul>
   *   <li>CVE-2018-10561 / CVE-2018-10562 – GPON router RCE (/GponForm/)</li>
   *   <li>Tenda/D-Link router RCE probes (/goform/)</li>
   *   <li>Generic WordPress probes (/wp-admin/, /wp-login.php, /wp-includes/)</li>
   *   <li>PHP info / eval probes (/phpinfo.php, /eval-stdin.php)</li>
   *   <li>Shell upload probes (/shell, /cmd, /c99.php, /c100.php)</li>
   *   <li>Admin panel probes (/admin/, /.env, /config/)</li>
   *   <li>Actuator abuse from external scanners (/actuator/ except /actuator/health)</li>
   *   <li>CGI vulnerability probes (/cgi-bin/)</li>
   *   <li>Crypto exchange probes (/mms-api/, /coins/)</li>
   *   <li>Gambling/VIP domain probes (/site/api/, /vipExclusiveDomain/)</li>
   *   <li>Chat room scanners (/join_room)</li>
   *   <li>Relay/proxy API probes (/relayApi/)</li>
   *   <li>Malware installer probes (/instatll, /install)</li>
   * </ul>
   */
  private static final List<String> BLOCKED_PATH_PREFIXES = List.of(
      // GPON / Tenda / D-Link router exploits
      "/GponForm/",
      "/Gpon/",
      "/goform/",
      // WordPress probes
      "/wp-admin/",
      "/wp-login.php",
      "/wp-includes/",
      "/wp-content/",
      // PHP probes
      "/phpinfo",
      "/eval-stdin.php",
      "/c99.php",
      "/c100.php",
      // Config/secrets probes
      "/.env",
      "/.git/",
      "/.svn/",
      "/.hg/",
      "/.DS_Store",
      "/config/",
      "/biz/server/config",
      // Shell/command probes
      "/shell",
      "/cmd",
      "/cgi-bin/",
      "/admin.php",
      // Crypto exchange / trading API probes
      "/mms-api/",
      "/coins/",
      "/exchange/",
      // Gambling / VIP domain probes
      "/site/api/",
      "/vipExclusiveDomain/",
      // Chat / WebSocket scanner probes
      "/join_room",
      // Relay / proxy API probes
      "/relayApi/",
      // Malware installer probes (note the intentional typo variant from real attacks)
      "/instatll",
      "/install",
      // Common Java/Spring exploit probes
      "/console/",
      "/manager/",
      "/solr/",
      "/druid/",
      "/nacos/"
  );

  /**
   * Regex patterns that match broader families of suspicious bot/scanner paths.
   * These catch variations not covered by exact prefixes.
   */
  private static final Pattern SUSPICIOUS_PATH_PATTERN = Pattern.compile(
      "(?i)"
      + "(/\\.(env|git|svn|htaccess|htpasswd|npmrc|dockerenv))"
      + "|(/wp-(admin|login|includes|content|json|cron))"
      + "|(\\.(php|asp|aspx|jsp|cgi)$)"
      + "|(/api/(v\\d+/)?public$)"
      + "|(/(admin|phpmyadmin|adminer|mysqladmin|pma)/)"
      + "|(/(backup|dump|db|database|sql|mysql|postgres)/)"
      + "|(/(test|debug|trace|status|info|metrics|env)/)"
  );

  // ---- IP-based rate limiting for suspicious requests ----

  /** Maximum number of blocked requests from a single IP before auto-banning. */
  private static final int SUSPICIOUS_IP_THRESHOLD = 10;

  /** Duration (seconds) an IP stays banned after exceeding the threshold. */
  private static final long BAN_DURATION_SECONDS = 3600; // 1 hour

  /** Window (seconds) within which hits are counted before reset. */
  private static final long COUNTER_WINDOW_SECONDS = 300; // 5 minutes

  /** Tracks per-IP suspicious request counters and ban timestamps. */
  private final Map<String, IpTracker> suspiciousIpMap = new ConcurrentHashMap<>();

  private static class IpTracker {
    final AtomicInteger hitCount = new AtomicInteger(0);
    volatile long windowStart = Instant.now().getEpochSecond();
    volatile long bannedUntil = 0;
  }

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Block well-known exploit/scanner paths before any further processing.
    // Resolve client IP only when needed (after path check) to avoid touching
    // headers prematurely.
    String requestURI = httpRequest.getRequestURI();
    if (isBlockedPath(requestURI) || isSuspiciousPath(requestURI)) {
      String clientIp = getClientIp(httpRequest);
      recordSuspiciousHit(clientIp);
      log.debug("Blocked request to known exploit path: {} {} from {}",
          httpRequest.getMethod(), requestURI, clientIp);
      httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
      httpResponse.setContentType("application/json");
      httpResponse.getWriter().write("{\"error\":\"Forbidden\"}");
      return;
    }

    // Check IP ban (for IPs that previously hit many suspicious paths)
    String clientIp = getClientIp(httpRequest);
    if (isIpBanned(clientIp)) {
      httpResponse.setStatus(HttpServletResponse.SC_FORBIDDEN);
      httpResponse.setContentType("application/json");
      httpResponse.getWriter().write("{\"error\":\"Forbidden\"}");
      return;
    }

    // Check all headers for malicious patterns
    Enumeration<String> headerNames = httpRequest.getHeaderNames();
    while (headerNames != null && headerNames.hasMoreElements()) {
      String headerName = headerNames.nextElement();
      String headerValue = httpRequest.getHeader(headerName);

      if (headerValue != null && isMalicious(headerName, headerValue)) {
        logSecurityEvent(httpRequest, headerName, headerValue);
        httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        httpResponse.setContentType("application/json");
        httpResponse.getWriter().write("{\"error\":\"Malicious request detected and blocked\"}");
        return;
      }
    }

    // Check request URI and query string for inject-style patterns
    String queryString = httpRequest.getQueryString();

    if (isMalicious("RequestURI", requestURI) || (queryString != null && isMalicious("QueryString", queryString))) {
      logSecurityEvent(httpRequest, "URI/Query", requestURI + "?" + queryString);
      httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      httpResponse.setContentType("application/json");
      httpResponse.getWriter().write("{\"error\":\"Malicious request detected and blocked\"}");
      return;
    }

    chain.doFilter(request, response);
  }

  private boolean isBlockedPath(String uri) {
    if (uri == null) {
      return false;
    }
    String lowerUri = uri.toLowerCase();
    for (String prefix : BLOCKED_PATH_PREFIXES) {
      if (lowerUri.startsWith(prefix.toLowerCase())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Check if the URI matches broader suspicious patterns (regex-based).
   *
   * <p>API paths ({@code /api/...}) are intentionally excluded from this broad
   * check because they are already protected by Spring Security's authentication
   * requirements. Applying broad patterns like {@code /(admin)/} or
   * {@code /(metrics)/} to API paths produces false positives for legitimate
   * application routes such as {@code /api/admin/settings} or
   * {@code /api/metrics/custom}. BLOCKED_PATH_PREFIXES still apply to all
   * paths (none of those prefixes start with {@code /api/}).
   */
  private boolean isSuspiciousPath(String uri) {
    if (uri == null) return false;
    // Skip the broad regex for application API paths – Spring Security handles
    // authentication/authorisation for these routes.
    if (uri.startsWith("/api/")) return false;
    return SUSPICIOUS_PATH_PATTERN.matcher(uri).find();
  }

  // ---- IP rate-limiting helpers ----

  private boolean isIpBanned(String ip) {
    if (ip == null) {
      return false;
    }
    IpTracker tracker = suspiciousIpMap.get(ip);
    if (tracker == null) {
      return false;
    }
    long now = Instant.now().getEpochSecond();
    if (tracker.bannedUntil > now) {
      return true;
    }
    // Ban expired – leave tracker for counter reuse; it resets on next window
    return false;
  }

  private void recordSuspiciousHit(String ip) {
    if (ip == null) {
      return;
    }
    long now = Instant.now().getEpochSecond();
    IpTracker tracker = suspiciousIpMap.computeIfAbsent(ip, k -> new IpTracker());

    // Reset counter if window expired
    if (now - tracker.windowStart > COUNTER_WINDOW_SECONDS) {
      tracker.hitCount.set(0);
      tracker.windowStart = now;
    }

    int hits = tracker.hitCount.incrementAndGet();
    if (hits >= SUSPICIOUS_IP_THRESHOLD && tracker.bannedUntil <= now) {
      tracker.bannedUntil = now + BAN_DURATION_SECONDS;
      log.warn("\uD83D\uDEA8 AUTO-BAN: IP {} banned for {} seconds after {} suspicious requests",
          ip, BAN_DURATION_SECONDS, hits);
    }

    // Periodically clean up stale entries (every ~100 hits across all IPs)
    if (suspiciousIpMap.size() > 1000) {
      evictStaleEntries(now);
    }
  }

  private void evictStaleEntries(long now) {
    Iterator<Map.Entry<String, IpTracker>> it = suspiciousIpMap.entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry<String, IpTracker> entry = it.next();
      IpTracker t = entry.getValue();
      // Evict if not banned and window is long expired
      if (t.bannedUntil <= now && now - t.windowStart > COUNTER_WINDOW_SECONDS * 2) {
        it.remove();
      }
    }
  }

  private boolean isMalicious(String headerName, String value) {
    if (value == null || value.isEmpty()) {
      return false;
    }

    // Check for JNDI injection (Log4Shell)
    if (JNDI_PATTERN.matcher(value).matches()) {
      return true;
    }

    // Check for XSS attempts in headers
    if (SCRIPT_INJECTION_PATTERN.matcher(value).matches()) {
      return true;
    }

    // Check for path traversal
    if (PATH_TRAVERSAL_PATTERN.matcher(value).matches()) {
      return true;
    }

    // More strict validation for forwarded headers (common attack vector)
    if (isSensitiveHeader(headerName)) {
      // Check for SQL injection patterns
      if (containsSuspiciousPatterns(value)) {
        return true;
      }
    }

    return false;
  }

  private boolean isSensitiveHeader(String headerName) {
    if (headerName == null) {
      return false;
    }
    for (String sensitiveHeader : SENSITIVE_HEADERS) {
      if (sensitiveHeader.equalsIgnoreCase(headerName)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsSuspiciousPatterns(String value) {
    // Check for base64 encoded commands (common in exploit attempts)
    if (value.length() > 1000) {
      return true; // Abnormally long header
    }

    // Check for encoded characters that might hide exploits
    if (value.contains("%00") || value.contains("\0")) {
      return true; // Null byte injection
    }

    // Check for LDAP injection patterns
    if (value.contains("ldap://") || value.contains("ldaps://") || value.contains("rmi://")
        || value.contains("dns://")) {
      return true;
    }

    return false;
  }

  private void logSecurityEvent(HttpServletRequest request, String headerName, String value) {
    String clientIp = getClientIp(request);
    String userAgent = request.getHeader("User-Agent");

    log.warn("🚨 SECURITY ALERT: Malicious request blocked");
    log.warn("Source IP: {}", clientIp);
    log.warn("Request URI: {}", request.getRequestURI());
    log.warn("Method: {}", request.getMethod());
    log.warn("Malicious Header/Field: {}", headerName);
    log.warn("Payload: {}", sanitizeForLogging(value));
    log.warn("User-Agent: {}", userAgent);

    // TODO: Consider integrating with security monitoring tools or sending alerts
    // For production: Send to SIEM, Datadog, or other monitoring systems
  }

  private String getClientIp(HttpServletRequest request) {
    return ClientIpResolver.resolve(request, trustedProxies);
  }

  private String sanitizeForLogging(String value) {
    if (value == null) {
      return "null";
    }
    // Truncate very long values and remove newlines for clean logging
    String sanitized = value.replaceAll("[\\r\\n]+", " ");
    if (sanitized.length() > 200) {
      return sanitized.substring(0, 200) + "... [truncated]";
    }
    return sanitized;
  }
}
