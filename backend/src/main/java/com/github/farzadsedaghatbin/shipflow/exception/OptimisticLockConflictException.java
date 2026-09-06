package com.github.farzadsedaghatbin.shipflow.exception;

import lombok.Getter;

/**
 * Thrown when a client-supplied {@code expectedVersion} does not match an entity's current
 * {@code @Version} column — i.e. someone else saved a change in between the client's last read
 * and this write. Mapped to HTTP 409 by {@link GlobalExceptionHandler}.
 *
 * <p>Carries the entity's current version and current state so the client can show/reconcile the
 * conflict without a second round-trip fetch.
 */
@Getter
public class OptimisticLockConflictException extends RuntimeException {

  /** One of {@code "PITCH"}, {@code "RETRO_ITEM"}, {@code "WIKI_PAGE"} — part of the fixed API contract. */
  private final String entityType;

  private final Long entityId;

  /** The entity's actual current version. May be {@code null} if it has never been saved/versioned. */
  private final Long currentVersion;

  /** The entity's current state (a response DTO), so the client can reconcile without re-fetching. */
  private final Object currentState;

  public OptimisticLockConflictException(
      String entityType, Long entityId, Long currentVersion, Object currentState) {
    super(
        "Optimistic lock conflict on "
            + entityType
            + " "
            + entityId
            + ": current version is "
            + currentVersion);
    this.entityType = entityType;
    this.entityId = entityId;
    this.currentVersion = currentVersion;
    this.currentState = currentState;
  }
}
