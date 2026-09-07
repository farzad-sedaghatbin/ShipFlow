package com.github.farzadsedaghatbin.shipflow.license;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Translates the current licence status ({@link LicenseService}) into the limits {@code
 * UserService} and {@code WorkflowAutomationService} enforce.
 *
 * <ul>
 *   <li>MISSING/EXPIRED (Community Edition): {@value #COMMUNITY_USER_SEAT_CAP} active users,
 *       {@value #COMMUNITY_AUTOMATION_CAP} enabled workflow automation rules.
 *   <li>VALID/GRACE: {@code floor(licensedSeats * 1.1)} active users (a small buffer over the
 *       purchased seat count), unlimited enabled automations.
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class LicenseLimits {

  /** Community Edition active-user cap (MISSING/EXPIRED licence status). */
  public static final int COMMUNITY_USER_SEAT_CAP = 10;

  /** Community Edition enabled-workflow-automation cap (MISSING/EXPIRED licence status). */
  public static final int COMMUNITY_AUTOMATION_CAP = 5;

  /** Buffer over the licensed seat count a VALID/GRACE licence allows — see class Javadoc. */
  private static final double LICENSED_SEAT_BUFFER = 1.1;

  private final LicenseService licenseService;

  private boolean isLicensed() {
    LicenseStatus status = licenseService.getStatus();
    return status == LicenseStatus.VALID || status == LicenseStatus.GRACE;
  }

  /** Maximum number of active users allowed right now. */
  public int activeUserCap() {
    if (isLicensed()) {
      return (int) Math.floor(licenseService.seatLimit() * LICENSED_SEAT_BUFFER);
    }
    return COMMUNITY_USER_SEAT_CAP;
  }

  /** Whether there is currently no cap on enabled workflow automation rules. */
  public boolean automationsUnlimited() {
    return isLicensed();
  }

  /** Maximum number of enabled workflow automation rules when {@link #automationsUnlimited()} is false. */
  public int enabledAutomationCap() {
    return COMMUNITY_AUTOMATION_CAP;
  }
}
