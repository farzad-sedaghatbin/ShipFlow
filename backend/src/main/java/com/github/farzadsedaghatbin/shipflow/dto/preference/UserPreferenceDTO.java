package com.github.farzadsedaghatbin.shipflow.dto.preference;

import com.github.farzadsedaghatbin.shipflow.entity.UserPreference.ThemeMode;
import java.time.LocalDateTime;
import lombok.*;

/** DTO for user preferences including theme settings. */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreferenceDTO {

  private Long id;
  private Long userId;
  private String username;

  /** Theme mode: LIGHT, DARK, SYSTEM */
  private ThemeMode themeMode;

  /** Primary color preference (hex code) */
  private String primaryColor;

  /** Secondary color preference (hex code) */
  private String secondaryColor;

  /** Compact view mode for lists */
  private Boolean compactView;

  /** Show hill chart animations */
  private Boolean enableAnimations;

  /** Whether Web Push notifications are delivered to this user's subscribed browsers */
  private Boolean pushEnabled;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
