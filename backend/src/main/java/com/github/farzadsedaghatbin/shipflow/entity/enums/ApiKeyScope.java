package com.github.farzadsedaghatbin.shipflow.entity.enums;

/**
 * Scopes that can be granted to an API key.
 */
public enum ApiKeyScope {
  /** Read-only access to all public API resources. */
  READ,
  /** Read + write access (create, update, delete). */
  WRITE,
  /** Full admin access including webhook & API key management. */
  ADMIN
}
