package com.github.farzadsedaghatbin.shipflow.service.audit;

import com.github.farzadsedaghatbin.shipflow.entity.AuthAuditLog;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditEventType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditOutcome;
import com.github.farzadsedaghatbin.shipflow.repository.AuthAuditLogRepository;
import com.github.farzadsedaghatbin.shipflow.security.ClientIpService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Records authentication events — who, from where, on what device, and whether
 * it worked.
 *
 * <p>Writes in a {@code REQUIRES_NEW} transaction so an audit row survives even
 * when the surrounding request fails and rolls back. A failed login has no
 * transaction of its own to join, and a failed login is precisely the event
 * most worth keeping.
 *
 * <p>Recording never propagates an exception. An audit trail that can take the
 * login endpoint down with it is worse than no audit trail, so every failure
 * here is logged and swallowed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthAuditService {

  /** Cloudflare's two-letter country header. Absent when not proxied by Cloudflare. */
  private static final String COUNTRY_HEADER = "CF-IPCountry";

  private static final int MAX_USER_AGENT = 512;
  private static final int MAX_REASON = 160;

  private final AuthAuditLogRepository repository;
  private final ClientIpService clientIpService;

  /** Records a successful event for the given username. */
  public void recordSuccess(AuthAuditEventType eventType, String username, Long userId) {
    record(eventType, AuthAuditOutcome.SUCCESS, username, userId, null);
  }

  /** Records a failed event. {@code failureReason} must never contain a password. */
  public void recordFailure(AuthAuditEventType eventType, String username, String failureReason) {
    record(eventType, AuthAuditOutcome.FAILURE, username, null, failureReason);
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void record(AuthAuditEventType eventType, AuthAuditOutcome outcome, String username,
      Long userId, String failureReason) {
    try {
      HttpServletRequest request = currentRequest();

      String userAgent = request != null ? request.getHeader("User-Agent") : null;
      String ip = request != null ? clientIpService.resolve(request) : null;
      String country = request != null ? request.getHeader(COUNTRY_HEADER) : null;

      AuthAuditLog entry = AuthAuditLog.builder()
          .eventType(eventType)
          .outcome(outcome)
          .username(username)
          .userId(userId)
          .ipAddress(ip)
          .country(normaliseCountry(country))
          .userAgent(truncate(userAgent, MAX_USER_AGENT))
          .deviceSummary(UserAgentSummary.summarise(userAgent))
          .failureReason(truncate(failureReason, MAX_REASON))
          .createdAt(OffsetDateTime.now())
          .build();

      repository.save(entry);
    } catch (Exception e) {
      // Never let auditing break authentication.
      log.error("Failed to record auth audit event {} for '{}'", eventType, username, e);
    }
  }

  /**
   * The servlet request bound to the current thread, or null when there is
   * none — scheduled jobs and tests authenticate outside a request.
   */
  private HttpServletRequest currentRequest() {
    if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
      return attrs.getRequest();
    }
    return null;
  }

  /** Cloudflare sends "XX" for unknown and "T1" for Tor; keep them, reject noise. */
  private String normaliseCountry(String country) {
    if (country == null || country.isBlank() || country.length() != 2) {
      return null;
    }
    return country.toUpperCase();
  }

  private String truncate(String value, int max) {
    if (value == null) {
      return null;
    }
    return value.length() <= max ? value : value.substring(0, max);
  }
}
