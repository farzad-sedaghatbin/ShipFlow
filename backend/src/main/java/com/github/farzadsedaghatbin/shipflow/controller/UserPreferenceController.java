package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.dto.preference.UpdatePreferenceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.preference.UserPreferenceDTO;
import com.github.farzadsedaghatbin.shipflow.entity.UserPreference.ThemeMode;
import com.github.farzadsedaghatbin.shipflow.service.UserPreferenceService;
import com.github.farzadsedaghatbin.shipflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/** REST controller for user preferences including theme settings. */
@RestController
@RequestMapping("/api/preferences")
@RequiredArgsConstructor
@Tag(name = "User Preferences", description = "Manage user preferences and theme settings")
public class UserPreferenceController {

  private final UserPreferenceService userPreferenceService;
  private final UserService userService;

  @GetMapping
  @Operation(summary = "Get user preferences", description = "Get current user's preferences including theme settings")
  public ResponseEntity<UserPreferenceDTO> getPreferences() {
    Long userId = getCurrentUserId();
    UserPreferenceDTO preferences = userPreferenceService.getPreferences(userId);
    return ResponseEntity.ok(preferences);
  }

  @PutMapping
  @Operation(summary = "Update preferences", description = "Update user preferences")
  public ResponseEntity<UserPreferenceDTO> updatePreferences(@RequestBody UpdatePreferenceRequest updateRequest) {
    Long userId = getCurrentUserId();
    UserPreferenceDTO preferences = userPreferenceService.updatePreferences(userId, updateRequest);
    return ResponseEntity.ok(preferences);
  }

  @PutMapping("/theme")
  @Operation(summary = "Update theme mode", description = "Update just the theme mode (LIGHT, DARK, SYSTEM)")
  public ResponseEntity<UserPreferenceDTO> updateTheme(@RequestBody Map<String, String> body) {
    Long userId = getCurrentUserId();
    ThemeMode themeMode = ThemeMode.valueOf(body.get("themeMode").toUpperCase());
    UserPreferenceDTO preferences = userPreferenceService.updateThemeMode(userId, themeMode);
    return ResponseEntity.ok(preferences);
  }

  @PostMapping("/reset")
  @Operation(summary = "Reset preferences", description = "Reset all preferences to defaults")
  public ResponseEntity<UserPreferenceDTO> resetPreferences() {
    Long userId = getCurrentUserId();
    UserPreferenceDTO preferences = userPreferenceService.resetToDefaults(userId);
    return ResponseEntity.ok(preferences);
  }

  private Long getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    return userService.findByUsername(username).getId();
  }
}
