package com.github.farzadsedaghatbin.shipflow.dto.preference;

import com.github.farzadsedaghatbin.shipflow.entity.UserPreference.ThemeMode;
import lombok.*;

/** Request DTO for updating user preferences. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePreferenceRequest {

  /** Theme mode: LIGHT, DARK, SYSTEM */
  private ThemeMode themeMode;

  /** Primary color preference (hex code, e.g., "#2563eb") */
  private String primaryColor;

  /** Secondary color preference (hex code) */
  private String secondaryColor;

  /** Compact view mode for lists */
  private Boolean compactView;

  /** Show hill chart animations */
  private Boolean enableAnimations;

  /** Whether Web Push notifications are delivered to this user's subscribed browsers */
  private Boolean pushEnabled;
}
