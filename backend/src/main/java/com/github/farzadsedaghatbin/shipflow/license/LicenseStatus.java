package com.github.farzadsedaghatbin.shipflow.license;

/**
 * Overall state of the currently-installed licence, as computed by
 * {@link LicenseService}.
 *
 * <ul>
 *   <li>{@link #MISSING} — no licence configured, no {@code app.license.public-key}
 *       configured, or the licence file failed signature/parse validation. This is
 *       the Community Edition state.
 *   <li>{@link #VALID} — a correctly-signed, unexpired licence.
 *   <li>{@link #GRACE} — a correctly-signed licence that expired less than 30 days
 *       ago. Behaves like {@link #VALID} for {@link LicenseService#hasFeature(String)}
 *       and seat limits, but the admin banner nags about it.
 *   <li>{@link #EXPIRED} — a correctly-signed licence that expired 30+ days ago.
 *       Community limits apply, same as {@link #MISSING}.
 * </ul>
 */
public enum LicenseStatus {
  MISSING,
  VALID,
  GRACE,
  EXPIRED
}
