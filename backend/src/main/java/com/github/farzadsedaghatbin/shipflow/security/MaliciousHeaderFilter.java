package com.github.farzadsedaghatbin.shipflow.security;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Security filter to detect and block malicious requests containing exploit
 * patterns such as Log4Shell (CVE-2021-44228), JNDI injection, GPON router
 * exploits (CVE-2018-10561/10562), and other common attack vectors.
 */
@Slf4j
@Component
public class MaliciousHeaderFilter implements Filter {

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
   *   <li>Generic WordPress probes (/wp-admin/, /wp-login.php, /wp-includes/)</li>
   *   <li>PHP info / eval probes (/phpinfo.php, /eval-stdin.php)</li>
   *   <li>Shell upload probes (/shell, /cmd, /c99.php, /c100.php)</li>
   *   <li>Admin panel probes (/admin/, /.env, /config/)</li>
   *   <li>Actuator abuse from external scanners (/actuator/ except /actuator/health)</li>
   *   <li>CGI vulnerability probes (/cgi-bin/)</li>
   * </ul>
   */
  private static final List<String> BLOCKED_PATH_PREFIXES = List.of(
      "/GponForm/",
      "/Gpon/",
      "/wp-admin/",
      "/wp-login.php",
      "/wp-includes/",
      "/wp-content/",
      "/phpinfo",
      "/eval-stdin.php",
      "/c99.php",
      "/c100.php",
      "/.env",
      "/.git/",
      "/shell",
      "/cmd",
      "/cgi-bin/",
      "/config/",
      "/admin.php"
  );

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    // Block well-known exploit/scanner paths before any further processing
    String requestURI = httpRequest.getRequestURI();
    if (isBlockedPath(requestURI)) {
      log.debug("Blocked request to known exploit path: {} {} from {}",
          httpRequest.getMethod(), requestURI, getClientIp(httpRequest));
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
    for (String prefix : BLOCKED_PATH_PREFIXES) {
      if (uri.startsWith(prefix)) {
        return true;
      }
    }
    return false;
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
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getHeader("X-Real-IP");
    }
    if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
      ip = request.getRemoteAddr();
    }
    return ip;
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
