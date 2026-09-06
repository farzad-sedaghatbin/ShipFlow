package com.github.farzadsedaghatbin.shipflow.event;

import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.enums.AuthAuditEventType;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.service.audit.AuthAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

/**
 * Bridges Spring Security's authentication events into the audit trail.
 *
 * <p>Listening to the framework's own events rather than instrumenting each
 * controller means every authentication path is covered by construction —
 * password login, passkey sign-in, and any future mechanism — with no
 * possibility of a new endpoint quietly skipping the audit.
 *
 * <p>Runs synchronously on the request thread. That is intentional: it keeps
 * {@code RequestContextHolder} available, which is where the client IP,
 * country and User-Agent come from. {@link AuthAuditService} swallows its own
 * failures, so a broken audit cannot fail a login.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthAuditEventListener {

  private final AuthAuditService authAuditService;
  private final UserRepository userRepository;

  @EventListener
  public void onAuthenticationSuccess(AuthenticationSuccessEvent event) {
    String username = event.getAuthentication().getName();

    Long userId = userRepository.findByUsername(username).map(User::getId).orElse(null);

    authAuditService.recordSuccess(AuthAuditEventType.LOGIN_SUCCESS, username, userId);
  }

  @EventListener
  public void onAuthenticationFailure(AbstractAuthenticationFailureEvent event) {
    String username = event.getAuthentication() != null ? String.valueOf(event.getAuthentication().getName()) : null;

    // The exception type is the reason (BadCredentialsException,
    // DisabledException, LockedException...). The message is deliberately not
    // used: it can echo submitted input back into the log.
    String reason = event.getException() != null ? event.getException().getClass().getSimpleName() : "Unknown";

    authAuditService.recordFailure(AuthAuditEventType.LOGIN_FAILURE, username, reason);
  }
}
