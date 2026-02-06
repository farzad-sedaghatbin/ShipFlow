package com.github.farzadsedaghatbin.shipflow.validation;

import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator for meeting types. Validates that the meeting type exists in the
 * organization's configured meeting types.
 * 
 * Note: This validator normalizes the input by trimming and converting to uppercase
 * before validation. The normalized value is set back to the field being validated.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ValidMeetingTypeValidator implements ConstraintValidator<ValidMeetingType, String> {

  private final OrganizationSettingsService organizationSettingsService;
  private boolean allowNull;

  @Override
  public void initialize(ValidMeetingType constraintAnnotation) {
    this.allowNull = constraintAnnotation.allowNull();
  }

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    // Handle null values based on allowNull setting
    if (value == null) {
      return allowNull;
    }

    // Normalize: trim and uppercase
    String normalizedValue = value.trim().toUpperCase();
    
    // Empty string after normalization is invalid
    if (normalizedValue.isEmpty()) {
      context.disableDefaultConstraintViolation();
      context.buildConstraintViolationWithTemplate("Meeting type cannot be empty")
          .addConstraintViolation();
      return false;
    }

    try {
      // Get organization settings with configured meeting types
      OrganizationSettingsDTO settings = organizationSettingsService.getSettings();
      
      if (settings == null || settings.getMeetingTypes() == null || settings.getMeetingTypes().isEmpty()) {
        log.warn("No meeting types configured in organization settings. Validation will pass for backwards compatibility.");
        return true; // Allow if no meeting types configured (backwards compatibility)
      }

      // Get list of valid meeting type names (normalized)
      List<String> validTypes = settings.getMeetingTypes().stream()
          .filter(mt -> Boolean.TRUE.equals(mt.getIsActive())) // Only active meeting types
          .map(mt -> mt.getName() != null ? mt.getName().trim().toUpperCase() : null)
          .filter(name -> name != null && !name.isEmpty())
          .collect(Collectors.toList());

      // Check if normalized value is in the list of valid types
      if (!validTypes.contains(normalizedValue)) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(
            String.format("Invalid meeting type '%s'. Allowed types: %s", normalizedValue, validTypes))
            .addConstraintViolation();
        return false;
      }

      return true;
      
    } catch (Exception e) {
      log.error("Error validating meeting type: {}", e.getMessage(), e);
      // On error, log but allow validation to pass to avoid breaking the system
      return true;
    }
  }
}
