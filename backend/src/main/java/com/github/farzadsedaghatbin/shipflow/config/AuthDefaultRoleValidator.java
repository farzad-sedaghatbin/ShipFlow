package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Startup validator for {@code app.auth.default-role} (env {@code
 * APP_AUTH_DEFAULT_ROLE}) — the role assigned to a publicly self-registered user
 * (see {@link com.github.farzadsedaghatbin.shipflow.service.UserService#createUser}).
 *
 * <p>{@code ADMIN} is never allowed as the default role, even if explicitly
 * configured — a public instance must not be able to mint admin accounts through
 * self-registration by misconfiguration. This is enforced here, once, at startup,
 * rather than re-checked on every request.
 *
 * <p>Follows the same {@link ApplicationListener} of {@link ApplicationReadyEvent}
 * / fail-fast-with-{@link IllegalStateException} pattern established by {@link
 * AirGappedModeValidator}. A syntactically invalid value (a string that isn't any
 * {@link UserRole} constant) already fails earlier, during context refresh, via
 * Spring's own String-to-enum conversion on the {@code @Value} binding below — this
 * validator only needs to cover the "valid enum constant, but disallowed" case.
 */
@Component
@Slf4j
public class AuthDefaultRoleValidator implements ApplicationListener<ApplicationReadyEvent> {

  private final UserRole defaultRole;

  public AuthDefaultRoleValidator(@Value("${app.auth.default-role:READONLY}") UserRole defaultRole) {
    this.defaultRole = defaultRole;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    validate();
  }

  /** Visible for testing — call directly with a constructed instance. */
  public void validate() {
    if (defaultRole == UserRole.ADMIN) {
      throw new IllegalStateException(
          "app.auth.default-role (APP_AUTH_DEFAULT_ROLE) is set to ADMIN. A publicly self-registered "
              + "user must never be able to become an admin — set it to MANAGER, MEMBER, or READONLY, "
              + "or leave it unset (defaults to READONLY).");
    }
  }
}
