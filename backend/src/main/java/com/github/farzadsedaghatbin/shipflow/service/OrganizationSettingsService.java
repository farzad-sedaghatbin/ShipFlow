package com.github.farzadsedaghatbin.shipflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.config.mcp.McpServerProperties;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.UpdateOrganizationSettingsRequest;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
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
  private final McpServerProperties mcpServerProperties;

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
    if (request.getDefaultSprintLengthWeeks() != null) {
      settings.setDefaultSprintLengthWeeks(request.getDefaultSprintLengthWeeks());
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
    if (request.getMeetingTypes() != null) {
      settings.setMeetingTypesJson(toJson(request.getMeetingTypes()));
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

    // Capacity Configuration
    if (request.getDefaultHoursPerDay() != null) {
      if (request.getDefaultHoursPerDay() < 1 || request.getDefaultHoursPerDay() > 24) {
        throw new IllegalArgumentException("Default hours per day must be between 1 and 24");
      }
      settings.setDefaultHoursPerDay(request.getDefaultHoursPerDay());
    }
    if (request.getDefaultWorkingDaysPerWeek() != null) {
      if (request.getDefaultWorkingDaysPerWeek() < 1 || request.getDefaultWorkingDaysPerWeek() > 7) {
        throw new IllegalArgumentException("Default working days per week must be between 1 and 7");
      }
      settings.setDefaultWorkingDaysPerWeek(request.getDefaultWorkingDaysPerWeek());
    }

    // Email / SMTP settings
    if (request.getEmailNotificationsEnabled() != null) {
      settings.setEmailNotificationsEnabled(request.getEmailNotificationsEnabled());
    }
    if (request.getSmtpHost() != null) {
      settings.setSmtpHost(request.getSmtpHost());
    }
    if (request.getSmtpPort() != null) {
      settings.setSmtpPort(request.getSmtpPort());
    }
    if (request.getSmtpUsername() != null) {
      settings.setSmtpUsername(request.getSmtpUsername());
    }
    if (request.getSmtpFrom() != null) {
      settings.setSmtpFrom(request.getSmtpFrom());
    }
    if (request.getSmtpTlsEnabled() != null) {
      settings.setSmtpTlsEnabled(request.getSmtpTlsEnabled());
    }

    // Wise Architecture Feature Flag
    if (request.getEnableWiseArchitecture() != null) {
      settings.setEnableWiseArchitecture(request.getEnableWiseArchitecture());
    }

    // Figma MCP Configuration (token only, managed via MCP settings API)
    if (request.getFigmaAccessToken() != null) {
      settings.setFigmaAccessToken(request.getFigmaAccessToken());
    }

    // GitHub MCP Configuration (token only, managed via MCP settings API)
    if (request.getGithubAccessToken() != null) {
      settings.setGithubAccessToken(request.getGithubAccessToken());
    }

    // Notion MCP Configuration (token only, managed via MCP settings API)
    if (request.getNotionAccessToken() != null) {
      settings.setNotionAccessToken(
          request.getNotionAccessToken().isBlank() ? null : request.getNotionAccessToken());
    }

    // Confluence MCP Configuration
    if (request.getConfluenceAccessToken() != null) {
      settings.setConfluenceAccessToken(
          request.getConfluenceAccessToken().isBlank() ? null : request.getConfluenceAccessToken());
    }
    if (request.getDefaultConfluenceDomain() != null) {
      settings.setDefaultConfluenceDomain(
          request.getDefaultConfluenceDomain().isBlank() ? null : request.getDefaultConfluenceDomain());
    }
    if (request.getDefaultConfluenceSpaceKey() != null) {
      settings.setDefaultConfluenceSpaceKey(
          request.getDefaultConfluenceSpaceKey().isBlank() ? null : request.getDefaultConfluenceSpaceKey());
    }

    // MCP Server runtime toggle (null = leave unchanged)
    if (request.getMcpServerEnabled() != null) {
      settings.setMcpServerEnabled(request.getMcpServerEnabled());
    }
    if (request.getMcpServerWriteEnabled() != null) {
      settings.setMcpServerWriteEnabled(request.getMcpServerWriteEnabled());
    }

    // SCIM toggle (null = leave unchanged)
    if (request.getScimEnabled() != null) {
      settings.setScimEnabled(request.getScimEnabled());
    }

    settings.setUpdatedBy(username);
    settings.setUpdatedAt(LocalDateTime.now());

    settings = settingsRepository.save(settings);
    log.info("Organization settings updated by {}", username);

    return toDTO(settings);
  }

  /**
   * Get Figma access token for MCP integration.
   * <p><strong>WARNING:</strong> This method returns a plaintext access token.
   * It is intended ONLY for internal use by MCP service components.
   * DO NOT expose this via REST endpoints or log the returned value.
   * Consider refactoring to use a dedicated token provider service.</p>
   * @return the Figma access token or null if not configured
   */
  public String getFigmaAccessToken() {
    return settingsRepository.findFirstByOrderByIdAsc()
        .map(OrganizationSettings::getFigmaAccessToken)
        .orElse(null);
  }

  /**
   * Get GitHub access token for MCP integration.
   * <p><strong>WARNING:</strong> This method returns a plaintext access token.
   * It is intended ONLY for internal use by MCP service components.
   * DO NOT expose this via REST endpoints or log the returned value.
   * Consider refactoring to use a dedicated token provider service.</p>
   * @return the GitHub access token or null if not configured
   */
  public String getGithubAccessToken() {
    return settingsRepository.findFirstByOrderByIdAsc()
        .map(OrganizationSettings::getGithubAccessToken)
        .orElse(null);
  }

  /**
   * Get Notion access token for MCP integration.
   * <p><strong>WARNING:</strong> This method returns a plaintext access token.
   * It is intended ONLY for internal use by MCP service components.
   * DO NOT expose this via REST endpoints or log the returned value.</p>
   * @return the Notion access token or null if not configured
   */
  public String getNotionAccessToken() {
    return settingsRepository.findFirstByOrderByIdAsc()
        .map(OrganizationSettings::getNotionAccessToken)
        .orElse(null);
  }

  /**
   * Get Confluence access token for MCP integration.
   * <p><strong>WARNING:</strong> This method returns a plaintext access token.
   * It is intended ONLY for internal use by MCP service components.
   * DO NOT expose this via REST endpoints or log the returned value.</p>
   * @return the Confluence access token or null if not configured
   */
  public String getConfluenceAccessToken() {
    return settingsRepository.findFirstByOrderByIdAsc()
        .map(OrganizationSettings::getConfluenceAccessToken)
        .orElse(null);
  }

  /**
   * Get the default Confluence space key for MCP integration.
   * @return the default Confluence space key or null if not configured
   */
  public String getConfluenceSpaceKey() {
    return settingsRepository.findFirstByOrderByIdAsc()
        .map(OrganizationSettings::getDefaultConfluenceSpaceKey)
        .orElse(null);
  }

  /** Reset settings to defaults. */
  @Transactional
  public OrganizationSettingsDTO resetToDefaults(String username) {
    settingsRepository.deleteAll();
    OrganizationSettings settings = createDefaultSettings(username);
    log.info("Organization settings reset to defaults by {}", username);
    return toDTO(settings);
  }

  /**
   * Generate a new SCIM bearer token. Stores the SHA-256 hash in the database and returns the raw
   * token once — callers must persist it; it cannot be recovered later.
   *
   * @param username admin who triggered the generation (for audit)
   * @return the raw Base64URL-encoded bearer token (shown once)
   */
  @Transactional
  public String generateScimToken(String username) {
    OrganizationSettings settings = settingsRepository.findFirstByOrderByIdAsc()
        .orElseGet(() -> createDefaultSettings(username));

    // Generate 32 cryptographically random bytes → Base64URL string
    SecureRandom rng = new SecureRandom();
    byte[] rawBytes = new byte[32];
    rng.nextBytes(rawBytes);
    String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawBytes);

    // Store only the SHA-256 hash
    settings.setScimTokenHash(sha256Hex(rawToken));
    settings.setScimEnabled(true);
    settings.setUpdatedBy(username);
    settings.setUpdatedAt(LocalDateTime.now());
    settingsRepository.save(settings);

    log.info("SCIM bearer token regenerated by {}", username);
    return rawToken;
  }

  /**
   * Verify a raw SCIM bearer token against the stored hash. Returns {@code true} if SCIM is
   * enabled and the token matches.
   */
  public boolean verifyScimToken(String rawToken) {
    return settingsRepository.findFirstByOrderByIdAsc()
        .map(s -> s.isScimEnabled()
            && s.getScimTokenHash() != null
            && s.getScimTokenHash().equals(sha256Hex(rawToken)))
        .orElse(false);
  }

  /** Compute a lowercase hex-encoded SHA-256 digest. */
  private static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
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
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("BLOCKER")
            .description("Blocks release or critical functionality").color("#7C3AED").isActive(true).order(1)
            .priority(1).build(),
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("CRITICAL")
            .description("System down or data loss").color("#DC2626").isActive(true).order(2).priority(2)
            .build(),
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("MAJOR")
            .description("Major feature broken").color("#F59E0B").isActive(true).order(3).priority(3).build(),
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("MINOR")
            .description("Minor feature issue").color("#3B82F6").isActive(true).order(4).priority(4).build(),
        OrganizationSettingsDTO.SeverityLevelConfig.builder().name("TRIVIAL")
            .description("Cosmetic or trivial issue").color("#10B981").isActive(true).order(5).priority(5)
            .build());

    // Default risk weights (balanced profile)
    OrganizationSettingsDTO.RiskWeights defaultRiskWeights = OrganizationSettingsDTO.RiskWeights.builder()
        .budgetWeight(25).bugsWeight(30).scopeWeight(25).timeWeight(20).build();

    // Default meeting types with DOR and DOD checklist items
    List<OrganizationSettingsDTO.MeetingTypeConfig> defaultMeetingTypes = createDefaultMeetingTypes();

    OrganizationSettings settings = OrganizationSettings.builder().organizationName("My Organization")
        .defaultCycleLengthWeeks(6).defaultCooldownWeeks(2).defaultSprintLengthWeeks(2).riskThresholdsJson(toJson(defaultRiskThresholds))
        .riskWeightsJson(toJson(defaultRiskWeights)).taskCategoriesJson(toJson(defaultTaskCategories))
        .pitchCategoriesJson(toJson(defaultPitchCategories)).colorsJson(toJson(defaultColors))
        .bugStatusesJson(toJson(defaultBugStatuses)).severityLevelsJson(toJson(defaultSeverityLevels))
        .meetingTypesJson(toJson(defaultMeetingTypes))
        .timeZone("UTC").dateFormat("MM/DD/YYYY").enableNotifications(true).enableAIFeatures(true)
        .emailNotificationsEnabled(true).smtpPort(587).smtpFrom("noreply@shipflow.dev").smtpTlsEnabled(true)
        .updatedBy(username).build();

    return settingsRepository.save(settings);
  }

  /** Create default meeting types based on the original hardcoded MeetingType enum. */
  private List<OrganizationSettingsDTO.MeetingTypeConfig> createDefaultMeetingTypes() {
    return List.of(
        // SHAPING meeting
        OrganizationSettingsDTO.MeetingTypeConfig.builder()
            .name("SHAPING").displayName("Shaping").description("Shape pitch ideas into concrete proposals")
            .color("#8B5CF6").isActive(true).order(1)
            .dorItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Problem Statement Defined")
                    .description("Clear problem statement is documented").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Stakeholders Identified")
                    .description("All relevant stakeholders are identified").isRequired(true).order(2).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Initial Research Complete")
                    .description("Background research has been done").isRequired(false).order(3).build()))
            .dodItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Solution Outlined")
                    .description("High-level solution approach documented").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Appetite Estimated")
                    .description("Time/effort appetite is defined").isRequired(true).order(2).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Risks Identified")
                    .description("Known risks and rabbit holes documented").isRequired(false).order(3).build()))
            .build(),

        // BETTING meeting
        OrganizationSettingsDTO.MeetingTypeConfig.builder()
            .name("BETTING").displayName("Betting").description("Decide which pitches to bet on for the cycle")
            .color("#F59E0B").isActive(true).order(2)
            .dorItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Pitches Shaped")
                    .description("All candidate pitches are fully shaped").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Team Availability Known")
                    .description("Team capacity for the cycle is clear").isRequired(true).order(2).build()))
            .dodItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Bets Selected")
                    .description("Pitches for the cycle are selected").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Teams Assigned")
                    .description("Teams are assigned to each bet").isRequired(true).order(2).build()))
            .build(),

        // KICKOFF meeting
        OrganizationSettingsDTO.MeetingTypeConfig.builder()
            .name("KICKOFF").displayName("Kickoff").description("Start work on a pitch with team alignment")
            .color("#10B981").isActive(true).order(3)
            .dorItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Pitch Documentation Ready")
                    .description("Full pitch documentation is available").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Team Members Present")
                    .description("All team members are attending").isRequired(true).order(2).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Acceptance Criteria Clear")
                    .description("Success criteria is defined").isRequired(false).order(3).build()))
            .dodItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Team Understands Scope")
                    .description("Team has clear understanding of the work").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Initial Tasks Created")
                    .description("First tasks are identified and created").isRequired(true).order(2).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Questions Addressed")
                    .description("Initial questions are answered").isRequired(false).order(3).build()))
            .build(),

        // STANDUP meeting
        OrganizationSettingsDTO.MeetingTypeConfig.builder()
            .name("STANDUP").displayName("Standup").description("Daily sync on progress and blockers")
            .color("#3B82F6").isActive(true).order(4)
            .dorItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Team Present")
                    .description("Team members are present").isRequired(true).order(1).build()))
            .dodItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Updates Shared")
                    .description("Each member shared their updates").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Blockers Identified")
                    .description("Any blockers are surfaced").isRequired(true).order(2).build()))
            .build(),

        // DEMO meeting
        OrganizationSettingsDTO.MeetingTypeConfig.builder()
            .name("DEMO").displayName("Demo").description("Demonstrate completed work to stakeholders")
            .color("#EC4899").isActive(true).order(5)
            .dorItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Demo Environment Ready")
                    .description("Demo environment is set up and working").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Features Complete")
                    .description("Features to demo are complete").isRequired(true).order(2).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Demo Script Prepared")
                    .description("Demo flow is planned").isRequired(false).order(3).build()))
            .dodItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Features Demonstrated")
                    .description("All planned features were shown").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Feedback Collected")
                    .description("Stakeholder feedback is documented").isRequired(true).order(2).build()))
            .build(),

        // RETROSPECTIVE meeting
        OrganizationSettingsDTO.MeetingTypeConfig.builder()
            .name("RETROSPECTIVE").displayName("Retrospective").description("Reflect on the cycle and identify improvements")
            .color("#6366F1").isActive(true).order(6)
            .dorItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Cycle Completed")
                    .description("The cycle work is finished").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Team Available")
                    .description("All team members can attend").isRequired(true).order(2).build()))
            .dodItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("What Went Well")
                    .description("Positive points are documented").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("What To Improve")
                    .description("Improvement areas are identified").isRequired(true).order(2).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Action Items Created")
                    .description("Concrete action items are defined").isRequired(true).order(3).build()))
            .build(),

        // HILL_CHART_REVIEW meeting
        OrganizationSettingsDTO.MeetingTypeConfig.builder()
            .name("HILL_CHART_REVIEW").displayName("Hill Chart Review").description("Review progress using hill charts")
            .color("#14B8A6").isActive(true).order(7)
            .dorItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Hill Charts Updated")
                    .description("All scopes have current hill positions").isRequired(true).order(1).build()))
            .dodItems(List.of(
                OrganizationSettingsDTO.DorDodItem.builder().name("Progress Reviewed")
                    .description("All hill chart positions discussed").isRequired(true).order(1).build(),
                OrganizationSettingsDTO.DorDodItem.builder().name("Risks Addressed")
                    .description("Stuck items have mitigation plans").isRequired(false).order(2).build()))
            .build()
    );
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

    // Provide default meeting types if not configured
    List<OrganizationSettingsDTO.MeetingTypeConfig> meetingTypes = fromJson(entity.getMeetingTypesJson(),
        new TypeReference<List<OrganizationSettingsDTO.MeetingTypeConfig>>() {
        });
    if (meetingTypes == null || meetingTypes.isEmpty()) {
      meetingTypes = createDefaultMeetingTypes();
    }
    
    // Filter out deleted DOR/DOD items from all meeting types
    if (meetingTypes != null) {
      meetingTypes.forEach(meetingType -> {
        if (meetingType.getDorItems() != null) {
          meetingType.setDorItems(meetingType.getDorItems().stream()
              .filter(item -> item.getIsDeleted() == null || !item.getIsDeleted())
              .collect(java.util.stream.Collectors.toList()));
        }
        if (meetingType.getDodItems() != null) {
          meetingType.setDodItems(meetingType.getDodItems().stream()
              .filter(item -> item.getIsDeleted() == null || !item.getIsDeleted())
              .collect(java.util.stream.Collectors.toList()));
        }
      });
    }

    return OrganizationSettingsDTO.builder().id(entity.getId()).organizationName(entity.getOrganizationName())
        .defaultCycleLengthWeeks(entity.getDefaultCycleLengthWeeks())
        .defaultCooldownWeeks(entity.getDefaultCooldownWeeks())
        .defaultSprintLengthWeeks(entity.getDefaultSprintLengthWeeks())
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
        .meetingTypes(meetingTypes)
        .timeZone(entity.getTimeZone()).dateFormat(entity.getDateFormat())
        .enableNotifications(entity.getEnableNotifications()).enableAIFeatures(entity.getEnableAIFeatures())
        // Email / SMTP settings
        .emailNotificationsEnabled(entity.getEmailNotificationsEnabled())
        .smtpHost(entity.getSmtpHost())
        .smtpPort(entity.getSmtpPort())
        .smtpUsername(entity.getSmtpUsername())
        .smtpFrom(entity.getSmtpFrom())
        .smtpTlsEnabled(entity.getSmtpTlsEnabled())
        .defaultHoursPerDay(entity.getDefaultHoursPerDay())
        .defaultWorkingDaysPerWeek(entity.getDefaultWorkingDaysPerWeek())
        // Wise Architecture Feature Flag
        .enableWiseArchitecture(entity.getEnableWiseArchitecture())
        // MCP Configuration (tokens not exposed, only presence flags)
        .hasFigmaAccessToken(entity.getFigmaAccessToken() != null && !entity.getFigmaAccessToken().isBlank())
        .hasGithubAccessToken(entity.getGithubAccessToken() != null && !entity.getGithubAccessToken().isBlank())
        .hasNotionAccessToken(entity.getNotionAccessToken() != null && !entity.getNotionAccessToken().isBlank())
        .hasConfluenceAccessToken(entity.getConfluenceAccessToken() != null && !entity.getConfluenceAccessToken().isBlank())
        .defaultConfluenceDomain(entity.getDefaultConfluenceDomain())
        .defaultConfluenceSpaceKey(entity.getDefaultConfluenceSpaceKey())
        // MCP Server runtime toggle — effective value: DB override if set, else env default.
        // Write mode is only effective when the server itself is enabled, mirroring
        // McpServerSettingsService.isWriteEnabled() so the DTO never reports a misleading
        // "write enabled" while the server is off.
        .mcpServerEnabled(resolveMcpEnabled(entity))
        .mcpServerWriteEnabled(resolveMcpEnabled(entity) && resolveMcpWriteEnabled(entity))
        // SCIM 2.0 (token hash never exposed — only the enabled flag)
        .scimEnabled(entity.isScimEnabled())
        .updatedAt(entity.getUpdatedAt()).updatedBy(entity.getUpdatedBy()).build();
  }

  /** Effective MCP server enabled state: DB override if set, else the env default. */
  private boolean resolveMcpEnabled(OrganizationSettings entity) {
    return entity.getMcpServerEnabled() != null
        ? entity.getMcpServerEnabled()
        : mcpServerProperties.isEnabled();
  }

  /** Effective MCP write-tools state ignoring the server-enabled gate (callers apply it). */
  private boolean resolveMcpWriteEnabled(OrganizationSettings entity) {
    return entity.getMcpServerWriteEnabled() != null
        ? entity.getMcpServerWriteEnabled()
        : mcpServerProperties.isWriteEnabled();
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
