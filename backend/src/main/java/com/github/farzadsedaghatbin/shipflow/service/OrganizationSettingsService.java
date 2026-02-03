package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.UpdateOrganizationSettingsRequest;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Service for managing organization-wide settings. */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrganizationSettingsService {

  private final OrganizationSettingsRepository settingsRepository;
  private final ObjectMapper objectMapper;

  /**
   * Get current organization settings. Creates default settings if none exist.
   */
  @Transactional
  public OrganizationSettingsDTO getSettings() {
    OrganizationSettings settings = settingsRepository.findFirstByOrderByIdAsc()
        .orElseGet(() -> createDefaultSettings("system"));
    return toDTO(settings);
  }

  /** Update organization settings. */
  @Transactional
  public OrganizationSettingsDTO updateSettings(UpdateOrganizationSettingsRequest request, String username) {
    OrganizationSettings settings = settingsRepository.findFirstByOrderByIdAsc()
        .orElseGet(() -> createDefaultSettings(username));

    // Update fields if provided
    if (request.getOrganizationName() != null) {
      settings.setOrganizationName(request.getOrganizationName());
    }
    if (request.getDefaultCycleLengthWeeks() != null) {
      settings.setDefaultCycleLengthWeeks(request.getDefaultCycleLengthWeeks());
    }
    if (request.getDefaultCooldownWeeks() != null) {
      settings.setDefaultCooldownWeeks(request.getDefaultCooldownWeeks());
    }
    if (request.getRiskThresholds() != null) {
      settings.setRiskThresholdsJson(toJson(request.getRiskThresholds()));
    }
    if (request.getRiskWeights() != null) {
      // Validate and normalize weights before saving
      OrganizationSettingsDTO.RiskWeights weights = request.getRiskWeights();
      if (!weights.isValid()) {
        log.warn("Risk weights don't sum to 100, normalizing: {}", weights);
        weights.normalize();
      }
      settings.setRiskWeightsJson(toJson(weights));
    }
    if (request.getTaskCategories() != null) {
      settings.setTaskCategoriesJson(toJson(request.getTaskCategories()));
    }
    if (request.getPitchCategories() != null) {
      settings.setPitchCategoriesJson(toJson(request.getPitchCategories()));
    }
    if (request.getColors() != null) {
      settings.setColorsJson(toJson(request.getColors()));
    }
    if (request.getBugStatuses() != null) {
      settings.setBugStatusesJson(toJson(request.getBugStatuses()));
    }
    if (request.getSeverityLevels() != null) {
      settings.setSeverityLevelsJson(toJson(request.getSeverityLevels()));
    }
    if (request.getTimeZone() != null) {
      settings.setTimeZone(request.getTimeZone());
    }
    if (request.getDateFormat() != null) {
      settings.setDateFormat(request.getDateFormat());
    }
    if (request.getEnableNotifications() != null) {
      settings.setEnableNotifications(request.getEnableNotifications());
    }
    if (request.getEnableAIFeatures() != null) {
      settings.setEnableAIFeatures(request.getEnableAIFeatures());
    }

    settings.setUpdatedBy(username);
    settings.setUpdatedAt(LocalDateTime.now());

    settings = settingsRepository.save(settings);
    log.info("Organization settings updated by {}", username);

    return toDTO(settings);
  }

  /** Reset settings to defaults. */
  @Transactional
  public OrganizationSettingsDTO resetToDefaults(String username) {
    settingsRepository.deleteAll();
    OrganizationSettings settings = createDefaultSettings(username);
    log.info("Organization settings reset to defaults by {}", username);
    return toDTO(settings);
  }

  /** Create default organization settings. */
  private OrganizationSettings createDefaultSettings(String username) {
    OrganizationSettingsDTO.RiskThresholds defaultRiskThresholds = OrganizationSettingsDTO.RiskThresholds.builder()
        .lowMax(30).mediumMax(60).highMax(85).build();

    List<OrganizationSettingsDTO.CategoryConfig> defaultTaskCategories = List.of(
        OrganizationSettingsDTO.CategoryConfig.builder().name("PITCH_SCOPE")
            .description("Work related to pitch deliverables").color("#3B82F6").isActive(true).order(1)
            .build(),
        OrganizationSettingsDTO.CategoryConfig.builder().name("DEBT_IMPROVEMENT")
            .description("Technical debt and improvements").color("#F59E0B").isActive(true).order(2)
            .build());

    List<OrganizationSettingsDTO.CategoryConfig> defaultPitchCategories = List.of(
        OrganizationSettingsDTO.CategoryConfig.builder().name("FEATURE").description("New feature development")
            .color("#10B981").isActive(true).order(1).build(),
        OrganizationSettingsDTO.CategoryConfig.builder().name("INFRASTRUCTURE")
            .description("Infrastructure and architecture").color("#6366F1").isActive(true).order(2)
            .build(),
        OrganizationSettingsDTO.CategoryConfig.builder().name("REFACTOR").description("Code refactoring")
            .color("#8B5CF6").isActive(true).order(3).build(),
        OrganizationSettingsDTO.CategoryConfig.builder().name("BUG_FIX").description("Bug fixes")
            .color("#EF4444").isActive(true).order(4).build());

    OrganizationSettingsDTO.ColorSettings defaultColors = OrganizationSettingsDTO.ColorSettings.builder()
        .appetiteHours("#3B82F6").actualHours("#10B981").overBudget("#EF4444").underBudget("#22C55E").build();

    List<OrganizationSettingsDTO.BugStatusConfig> defaultBugStatuses = List.of(
        OrganizationSettingsDTO.BugStatusConfig.builder().name("NEW").description("Newly reported")
            .color("#3B82F6").isActive(true).order(1).isClosed(false).build(),
        OrganizationSettingsDTO.BugStatusConfig.builder().name("IN_PROGRESS").description("Being worked on")
            .color("#F59E0B").isActive(true).order(2).isClosed(false).build(),
        OrganizationSettingsDTO.BugStatusConfig.builder().name("FIXED").description("Fix implemented")
            .color("#10B981").isActive(true).order(3).isClosed(true).build(),
        OrganizationSettingsDTO.BugStatusConfig.builder().name("VERIFIED").description("Fix verified")
            .color("#22C55E").isActive(true).order(4).isClosed(true).build(),
        OrganizationSettingsDTO.BugStatusConfig.builder().name("WONT_FIX").description("Will not fix")
            .color("#6B7280").isActive(true).order(5).isClosed(true).build());

    List<OrganizationSettingsDTO.SeverityLevelConfig> defaultSeverityLevels = List.of(
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("CRITICAL")
            .description("System down or data loss").color("#DC2626").isActive(true).order(1).priority(1)
            .build(),
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("HIGH").description("Major feature broken")
            .color("#F59E0B").isActive(true).order(2).priority(2).build(),
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("MEDIUM")
            .description("Feature partially broken").color("#3B82F6").isActive(true).order(3).priority(3)
            .build(),
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("LOW").description("Minor issue or cosmetic")
            .color("#10B981").isActive(true).order(4).priority(4).build());

    // Default risk weights (balanced profile)
    OrganizationSettingsDTO.RiskWeights defaultRiskWeights = OrganizationSettingsDTO.RiskWeights.builder()
        .budgetWeight(25).bugsWeight(30).scopeWeight(25).timeWeight(20).build();

    OrganizationSettings settings = OrganizationSettings.builder().organizationName("My Organization")
        .defaultCycleLengthWeeks(6).defaultCooldownWeeks(2).riskThresholdsJson(toJson(defaultRiskThresholds))
        .riskWeightsJson(toJson(defaultRiskWeights)).taskCategoriesJson(toJson(defaultTaskCategories))
        .pitchCategoriesJson(toJson(defaultPitchCategories)).colorsJson(toJson(defaultColors))
        .bugStatusesJson(toJson(defaultBugStatuses)).severityLevelsJson(toJson(defaultSeverityLevels))
        .timeZone("UTC").dateFormat("MM/DD/YYYY").enableNotifications(true).enableAIFeatures(true)
        .updatedBy(username).build();

    return settingsRepository.save(settings);
  }

  /** Convert entity to DTO. */
  private OrganizationSettingsDTO toDTO(OrganizationSettings entity) {
    // Provide default risk weights if not configured
    OrganizationSettingsDTO.RiskWeights weights = fromJson(entity.getRiskWeightsJson(),
        new TypeReference<OrganizationSettingsDTO.RiskWeights>() {
        });
    if (weights == null) {
      weights = OrganizationSettingsDTO.RiskWeights.builder().budgetWeight(25).bugsWeight(30).scopeWeight(25)
          .timeWeight(20).build();
    }

    return OrganizationSettingsDTO.builder().id(entity.getId()).organizationName(entity.getOrganizationName())
        .defaultCycleLengthWeeks(entity.getDefaultCycleLengthWeeks())
        .defaultCooldownWeeks(entity.getDefaultCooldownWeeks())
        .riskThresholds(fromJson(entity.getRiskThresholdsJson(),
            new TypeReference<OrganizationSettingsDTO.RiskThresholds>() {
            }))
        .riskWeights(weights).taskCategories(fromJson(entity.getTaskCategoriesJson(),
            new TypeReference<List<OrganizationSettingsDTO.CategoryConfig>>() {
            }))
        .pitchCategories(fromJson(entity.getPitchCategoriesJson(),
            new TypeReference<List<OrganizationSettingsDTO.CategoryConfig>>() {
            }))
        .colors(fromJson(entity.getColorsJson(), new TypeReference<OrganizationSettingsDTO.ColorSettings>() {
        })).bugStatuses(fromJson(entity.getBugStatusesJson(),
            new TypeReference<List<OrganizationSettingsDTO.BugStatusConfig>>() {
            }))
        .severityLevels(fromJson(entity.getSeverityLevelsJson(),
            new TypeReference<List<OrganizationSettingsDTO.SeverityLevelConfig>>() {
            }))
        .timeZone(entity.getTimeZone()).dateFormat(entity.getDateFormat())
        .enableNotifications(entity.getEnableNotifications()).enableAIFeatures(entity.getEnableAIFeatures())
        .updatedAt(entity.getUpdatedAt()).updatedBy(entity.getUpdatedBy()).build();
  }

  /** Convert object to JSON string. */
  private String toJson(Object obj) {
    try {
      return objectMapper.writeValueAsString(obj);
    } catch (JsonProcessingException e) {
      log.error("Failed to serialize object to JSON", e);
      return "{}";
    }
  }

  /** Convert JSON string to object. */
  private <T> T fromJson(String json, TypeReference<T> typeRef) {
    if (json == null || json.isEmpty()) {
      return null;
    }
    try {
      return objectMapper.readValue(json, typeRef);
    } catch (JsonProcessingException e) {
      log.error("Failed to deserialize JSON", e);
      return null;
    }
  }
}
