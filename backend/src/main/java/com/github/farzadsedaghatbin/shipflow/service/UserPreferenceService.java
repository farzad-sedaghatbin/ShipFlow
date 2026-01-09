package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.preference.UpdatePreferenceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.preference.UserPreferenceDTO;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserPreference;
import com.github.farzadsedaghatbin.shipflow.entity.UserPreference.ThemeMode;
import com.github.farzadsedaghatbin.shipflow.repository.UserPreferenceRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing user preferences including theme settings.
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class UserPreferenceService {

    private final UserPreferenceRepository userPreferenceRepository;
    private final UserRepository userRepository;

    /**
     * Get user preferences, creating defaults if not exists.
     */
    public UserPreferenceDTO getPreferences(Long userId) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        return toDTO(preference);
    }

    /**
     * Update user preferences.
     */
    public UserPreferenceDTO updatePreferences(Long userId, UpdatePreferenceRequest request) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));

        if (request.getThemeMode() != null) {
            preference.setThemeMode(request.getThemeMode());
        }
        if (request.getPrimaryColor() != null) {
            preference.setPrimaryColor(request.getPrimaryColor());
        }
        if (request.getSecondaryColor() != null) {
            preference.setSecondaryColor(request.getSecondaryColor());
        }
        if (request.getCompactView() != null) {
            preference.setCompactView(request.getCompactView());
        }
        if (request.getEnableAnimations() != null) {
            preference.setEnableAnimations(request.getEnableAnimations());
        }

        UserPreference saved = userPreferenceRepository.save(preference);
        log.info("User preferences updated for user {}", userId);
        return toDTO(saved);
    }

    /**
     * Update just the theme mode.
     */
    public UserPreferenceDTO updateThemeMode(Long userId, ThemeMode themeMode) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        
        preference.setThemeMode(themeMode);
        UserPreference saved = userPreferenceRepository.save(preference);
        log.info("Theme mode updated to {} for user {}", themeMode, userId);
        return toDTO(saved);
    }

    /**
     * Reset preferences to defaults.
     */
    public UserPreferenceDTO resetToDefaults(Long userId) {
        UserPreference preference = userPreferenceRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultPreferences(userId));
        
        preference.setThemeMode(ThemeMode.SYSTEM);
        preference.setPrimaryColor(null);
        preference.setSecondaryColor(null);
        preference.setCompactView(false);
        preference.setEnableAnimations(true);

        UserPreference saved = userPreferenceRepository.save(preference);
        log.info("Preferences reset to defaults for user {}", userId);
        return toDTO(saved);
    }

    private UserPreference createDefaultPreferences(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        UserPreference preference = UserPreference.builder()
                .user(user)
                .themeMode(ThemeMode.SYSTEM)
                .compactView(false)
                .enableAnimations(true)
                .build();

        return userPreferenceRepository.save(preference);
    }

    private UserPreferenceDTO toDTO(UserPreference preference) {
        return UserPreferenceDTO.builder()
                .id(preference.getId())
                .userId(preference.getUser().getId())
                .username(preference.getUser().getUsername())
                .themeMode(preference.getThemeMode())
                .primaryColor(preference.getPrimaryColor())
                .secondaryColor(preference.getSecondaryColor())
                .compactView(preference.getCompactView())
                .enableAnimations(preference.getEnableAnimations())
                .createdAt(preference.getCreatedAt())
                .updatedAt(preference.getUpdatedAt())
                .build();
    }
}
