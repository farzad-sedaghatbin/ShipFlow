package com.github.farzadsedaghatbin.shipflow.license;

/**
 * Thrown by {@link LicenseService#uploadLicense(String)} when the uploaded content is malformed
 * JSON, missing {@code payload}/{@code signature}, or fails Ed25519 signature verification.
 * Mapped to HTTP 400 (messageKey {@code license.invalid}) by {@code GlobalExceptionHandler}.
 */
public class LicenseInvalidException extends RuntimeException {

  public LicenseInvalidException(String message) {
    super(message);
  }
}
