package com.github.farzadsedaghatbin.shipflow.license;

import lombok.Getter;

/**
 * Thrown by {@link LicenseLimits#assertSeatAvailable(long)} when creating or reactivating a user
 * would exceed the active-user seat cap computed by {@link LicenseLimits#activeUserCap()} —
 * Community Edition default {@value LicenseLimits#COMMUNITY_USER_SEAT_CAP}, or {@code
 * floor(licensedSeats * 1.1)} under a VALID/GRACE licence. Every path that creates a new active
 * user or reactivates an inactive one goes through that shared assertion — {@code UserService},
 * {@code SsoService}, and {@code ScimService}. Never causes an existing user to be deactivated.
 * Mapped to HTTP 409 (messageKey {@code license.seats.exceeded}) by {@code GlobalExceptionHandler}.
 */
@Getter
public class SeatLimitExceededException extends RuntimeException {

  private final int limit;

  public SeatLimitExceededException(int limit) {
    super("Active user seat limit reached (" + limit + ")");
    this.limit = limit;
  }
}
