package com.github.farzadsedaghatbin.shipflow.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The organization's default cycle/cooldown/sprint lengths, in weeks.
 *
 * <p>
 * A deliberately tiny slice of {@link OrganizationSettingsDTO}. The full settings
 * endpoint is ADMIN-only because it carries integration configuration; the cycle
 * form only needs these three plain numbers, and it is used by MANAGERs. Exposing
 * just these keeps the admin endpoint closed while letting a manager's form pick
 * up the organization's configured lengths instead of silently falling back to
 * hard-coded ones.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CycleDefaultsDTO {

  private Integer defaultCycleLengthWeeks;
  private Integer defaultCooldownWeeks;
  private Integer defaultSprintLengthWeeks;
}
