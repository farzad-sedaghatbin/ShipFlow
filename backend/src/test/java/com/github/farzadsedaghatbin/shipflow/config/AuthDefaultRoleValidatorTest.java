package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import org.junit.jupiter.api.Test;

/**
 * Lightweight unit tests for the app.auth.default-role startup validator.
 * Constructed directly — no Spring context required, mirroring
 * {@link AirGappedModeValidatorTest}'s style.
 */
class AuthDefaultRoleValidatorTest {

  @Test
  void validate_DefaultRoleIsAdmin_ThrowsIllegalStateException() {
    AuthDefaultRoleValidator validator = new AuthDefaultRoleValidator(UserRole.ADMIN);

    IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
    assertThat(ex.getMessage()).contains("app.auth.default-role").contains("ADMIN");
  }

  @Test
  void validate_DefaultRoleIsManager_DoesNotThrow() {
    AuthDefaultRoleValidator validator = new AuthDefaultRoleValidator(UserRole.MANAGER);
    assertDoesNotThrow(validator::validate);
  }

  @Test
  void validate_DefaultRoleIsMember_DoesNotThrow() {
    AuthDefaultRoleValidator validator = new AuthDefaultRoleValidator(UserRole.MEMBER);
    assertDoesNotThrow(validator::validate);
  }

  @Test
  void validate_DefaultRoleIsReadonly_DoesNotThrow() {
    AuthDefaultRoleValidator validator = new AuthDefaultRoleValidator(UserRole.READONLY);
    assertDoesNotThrow(validator::validate);
  }
}
