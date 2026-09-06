package com.github.farzadsedaghatbin.shipflow.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a non-admin caller hits {@code POST /api/auth/register} while
 * {@code app.auth.public-registration} is disabled. An authenticated ADMIN
 * caller (the existing "Add User" flow in User Management, which reuses this
 * same endpoint) is never subject to this check.
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class RegistrationDisabledException extends RuntimeException {

  public RegistrationDisabledException(String message) {
    super(message);
  }

  public RegistrationDisabledException(String message, Throwable cause) {
    super(message, cause);
  }
}
