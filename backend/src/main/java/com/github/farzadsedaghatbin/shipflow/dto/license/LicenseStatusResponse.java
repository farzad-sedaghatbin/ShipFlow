package com.github.farzadsedaghatbin.shipflow.dto.license;

import com.github.farzadsedaghatbin.shipflow.license.LicenseStatus;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code GET /api/license/status} response — also returned by the upload/remove endpoints so the
 * frontend can update from the mutation response without an extra round trip.
 *
 * <p>{@code communityCap}/{@code automationCap} are always the Community Edition constants ({@link
 * com.github.farzadsedaghatbin.shipflow.license.LicenseLimits#COMMUNITY_USER_SEAT_CAP}/{@link
 * com.github.farzadsedaghatbin.shipflow.license.LicenseLimits#COMMUNITY_AUTOMATION_CAP}), regardless
 * of {@code status} — the frontend uses {@code status} to decide whether to show them as the active
 * limit (MISSING/EXPIRED) or as informational "what Community Edition allows" context (VALID/GRACE).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LicenseStatusResponse {

  private LicenseStatus status;
  private String edition;
  private String licensee;

  /** Licensed seat count from the payload, or {@code null} when there is no licence. */
  private Integer seats;

  private long seatsUsed;
  private List<String> features;
  private LocalDate issuedAt;
  private LocalDate expiresAt;
  private LocalDate supportUntil;

  /** {@code expiresAt + 30 days}; only meaningful while {@code status == GRACE}. */
  private LocalDate graceEndsAt;

  private int communityCap;
  private int automationCap;
  private long automationsUsed;
}
