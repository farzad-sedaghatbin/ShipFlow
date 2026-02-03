package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.admin.OrganizationSettingsDTO;
import com.github.farzadsedaghatbin.shipflow.dto.admin.UpdateOrganizationSettingsRequest;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for OrganizationSettingsService. Tests cover: - Retrieving
 * settings (existing and default creation) - Updating settings (full and
 * partial updates) - Resetting to defaults - JSON serialization/deserialization
 * - Audit trail tracking
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrganizationSettingsService Tests")
class OrganizationSettingsServiceTest {

  @Mock
  private OrganizationSettingsRepository settingsRepository;

  @Mock
  private ObjectMapper objectMapper;

  @InjectMocks
  private OrganizationSettingsService settingsService;

  private OrganizationSettings testSettings;
  private String testUsername = "admin";

  @BeforeEach
  void setUp() {
    // Use real ObjectMapper for these tests
    settingsService = new OrganizationSettingsService(settingsRepository, new ObjectMapper());

    testSettings = OrganizationSettings.builder().id(1L).organizationName("Test Org").defaultCycleLengthWeeks(6)
        .defaultCooldownWeeks(2).riskThresholdsJson("{\"lowMax\":30,\"mediumMax\":60,\"highMax\":85}")
        .taskCategoriesJson(
            "[{\"name\":\"PITCH_SCOPE\",\"description\":\"Pitch work\",\"color\":\"#3B82F6\",\"isActive\":true,\"order\":1}]")
        .pitchCategoriesJson(
            "[{\"name\":\"FEATURE\",\"description\":\"New features\",\"color\":\"#10B981\",\"isActive\":true,\"order\":1}]")
        .timeZone("UTC").dateFormat("MM/DD/YYYY").enableNotifications(true).enableAIFeatures(true)
        .updatedBy(testUsername).updatedAt(LocalDateTime.now()).build();
  }

  @Nested
  @DisplayName("Get Settings Tests")
  class GetSettingsTests {

    @Test
    @DisplayName("Should return existing settings when they exist")
    void shouldReturnExistingSettings() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));

      // When
      OrganizationSettingsDTO result = settingsService.getSettings();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(1L);
      assertThat(result.getOrganizationName()).isEqualTo("Test Org");
      assertThat(result.getDefaultCycleLengthWeeks()).isEqualTo(6);
      assertThat(result.getDefaultCooldownWeeks()).isEqualTo(2);
      assertThat(result.getRiskThresholds()).isNotNull();
      assertThat(result.getRiskThresholds().getLowMax()).isEqualTo(30);
      assertThat(result.getRiskThresholds().getMediumMax()).isEqualTo(60);
      assertThat(result.getRiskThresholds().getHighMax()).isEqualTo(85);
      assertThat(result.getTaskCategories()).isNotEmpty();
      assertThat(result.getPitchCategories()).isNotEmpty();
      assertThat(result.getTimeZone()).isEqualTo("UTC");
      assertThat(result.getDateFormat()).isEqualTo("MM/DD/YYYY");
      assertThat(result.getEnableNotifications()).isTrue();
      assertThat(result.getEnableAIFeatures()).isTrue();

      verify(settingsRepository).findFirstByOrderByIdAsc();
    }

    @Test
    @DisplayName("Should create and return default settings when none exist")
    void shouldCreateDefaultSettingsWhenNoneExist() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      OrganizationSettingsDTO result = settingsService.getSettings();

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getOrganizationName()).isEqualTo("My Organization");
      assertThat(result.getDefaultCycleLengthWeeks()).isEqualTo(6);
      assertThat(result.getDefaultCooldownWeeks()).isEqualTo(2);
      assertThat(result.getRiskThresholds()).isNotNull();
      assertThat(result.getRiskThresholds().getLowMax()).isEqualTo(30);
      assertThat(result.getTaskCategories()).hasSize(2);
      assertThat(result.getPitchCategories()).hasSize(4);
      assertThat(result.getEnableNotifications()).isTrue();
      assertThat(result.getEnableAIFeatures()).isTrue();

      ArgumentCaptor<OrganizationSettings> captor = ArgumentCaptor.forClass(OrganizationSettings.class);
      verify(settingsRepository).save(captor.capture());
      OrganizationSettings saved = captor.getValue();
      assertThat(saved.getOrganizationName()).isEqualTo("My Organization");
    }

    @Test
    @DisplayName("Should deserialize task categories correctly")
    void shouldDeserializeTaskCategoriesCorrectly() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));

      // When
      OrganizationSettingsDTO result = settingsService.getSettings();

      // Then
      assertThat(result.getTaskCategories()).isNotNull();
      assertThat(result.getTaskCategories()).hasSize(1);
      assertThat(result.getTaskCategories().get(0).getName()).isEqualTo("PITCH_SCOPE");
      assertThat(result.getTaskCategories().get(0).getDescription()).isEqualTo("Pitch work");
      assertThat(result.getTaskCategories().get(0).getColor()).isEqualTo("#3B82F6");
      assertThat(result.getTaskCategories().get(0).getIsActive()).isTrue();
    }

    @Test
    @DisplayName("Should deserialize pitch categories correctly")
    void shouldDeserializePitchCategoriesCorrectly() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));

      // When
      OrganizationSettingsDTO result = settingsService.getSettings();

      // Then
      assertThat(result.getPitchCategories()).isNotNull();
      assertThat(result.getPitchCategories()).hasSize(1);
      assertThat(result.getPitchCategories().get(0).getName()).isEqualTo("FEATURE");
      assertThat(result.getPitchCategories().get(0).getDescription()).isEqualTo("New features");
      assertThat(result.getPitchCategories().get(0).getColor()).isEqualTo("#10B981");
    }
  }

  @Nested
  @DisplayName("Update Settings Tests")
  class UpdateSettingsTests {

    @Test
    @DisplayName("Should update organization name")
    void shouldUpdateOrganizationName() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
      when(settingsRepository.save(any(OrganizationSettings.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .organizationName("New Organization Name").build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, testUsername);

      // Then
      assertThat(result.getOrganizationName()).isEqualTo("New Organization Name");

      ArgumentCaptor<OrganizationSettings> captor = ArgumentCaptor.forClass(OrganizationSettings.class);
      verify(settingsRepository).save(captor.capture());
      assertThat(captor.getValue().getOrganizationName()).isEqualTo("New Organization Name");
      assertThat(captor.getValue().getUpdatedBy()).isEqualTo(testUsername);
    }

    @Test
    @DisplayName("Should update cycle configuration")
    void shouldUpdateCycleConfiguration() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
      when(settingsRepository.save(any(OrganizationSettings.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .defaultCycleLengthWeeks(8).defaultCooldownWeeks(3).build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, testUsername);

      // Then
      assertThat(result.getDefaultCycleLengthWeeks()).isEqualTo(8);
      assertThat(result.getDefaultCooldownWeeks()).isEqualTo(3);
    }

    @Test
    @DisplayName("Should update risk thresholds")
    void shouldUpdateRiskThresholds() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
      when(settingsRepository.save(any(OrganizationSettings.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      OrganizationSettingsDTO.RiskThresholds newThresholds = OrganizationSettingsDTO.RiskThresholds.builder()
          .lowMax(25).mediumMax(55).highMax(80).build();

      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .riskThresholds(newThresholds).build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, testUsername);

      // Then
      assertThat(result.getRiskThresholds()).isNotNull();
      assertThat(result.getRiskThresholds().getLowMax()).isEqualTo(25);
      assertThat(result.getRiskThresholds().getMediumMax()).isEqualTo(55);
      assertThat(result.getRiskThresholds().getHighMax()).isEqualTo(80);
    }

    @Test
    @DisplayName("Should update task categories")
    void shouldUpdateTaskCategories() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
      when(settingsRepository.save(any(OrganizationSettings.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      List<OrganizationSettingsDTO.CategoryConfig> newCategories = List
          .of(OrganizationSettingsDTO.CategoryConfig.builder().name("CUSTOM_CATEGORY")
              .description("Custom work").color("#FF5733").isActive(true).order(1).build());

      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .taskCategories(newCategories).build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, testUsername);

      // Then
      assertThat(result.getTaskCategories()).hasSize(1);
      assertThat(result.getTaskCategories().get(0).getName()).isEqualTo("CUSTOM_CATEGORY");
      assertThat(result.getTaskCategories().get(0).getColor()).isEqualTo("#FF5733");
    }

    @Test
    @DisplayName("Should update feature toggles")
    void shouldUpdateFeatureToggles() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
      when(settingsRepository.save(any(OrganizationSettings.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .enableNotifications(false).enableAIFeatures(false).build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, testUsername);

      // Then
      assertThat(result.getEnableNotifications()).isFalse();
      assertThat(result.getEnableAIFeatures()).isFalse();
    }

    @Test
    @DisplayName("Should update only provided fields (partial update)")
    void shouldUpdateOnlyProvidedFields() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
      when(settingsRepository.save(any(OrganizationSettings.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .organizationName("Partial Update")
          // Other fields not provided
          .build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, testUsername);

      // Then
      assertThat(result.getOrganizationName()).isEqualTo("Partial Update");
      // Original values should be preserved
      assertThat(result.getDefaultCycleLengthWeeks()).isEqualTo(6);
      assertThat(result.getDefaultCooldownWeeks()).isEqualTo(2);
      assertThat(result.getTimeZone()).isEqualTo("UTC");
    }

    @Test
    @DisplayName("Should track updated by username")
    void shouldTrackUpdatedByUsername() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(testSettings));
      when(settingsRepository.save(any(OrganizationSettings.class)))
          .thenAnswer(invocation -> invocation.getArgument(0));

      String specificUser = "specific-admin";
      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .organizationName("Test").build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, specificUser);

      // Then
      assertThat(result.getUpdatedBy()).isEqualTo(specificUser);
    }

    @Test
    @DisplayName("Should create default settings if none exist when updating")
    void shouldCreateDefaultSettingsIfNoneExistWhenUpdating() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      UpdateOrganizationSettingsRequest request = UpdateOrganizationSettingsRequest.builder()
          .organizationName("New Org").build();

      // When
      OrganizationSettingsDTO result = settingsService.updateSettings(request, testUsername);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getOrganizationName()).isEqualTo("New Org");
      verify(settingsRepository, times(2)).save(any(OrganizationSettings.class)); // Create + Update
    }
  }

  @Nested
  @DisplayName("Reset to Defaults Tests")
  class ResetToDefaultsTests {

    @Test
    @DisplayName("Should delete existing settings and create defaults")
    void shouldDeleteExistingAndCreateDefaults() {
      // Given
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      OrganizationSettingsDTO result = settingsService.resetToDefaults(testUsername);

      // Then
      assertThat(result).isNotNull();
      assertThat(result.getOrganizationName()).isEqualTo("My Organization");
      assertThat(result.getDefaultCycleLengthWeeks()).isEqualTo(6);
      assertThat(result.getDefaultCooldownWeeks()).isEqualTo(2);
      assertThat(result.getRiskThresholds().getLowMax()).isEqualTo(30);
      assertThat(result.getRiskThresholds().getMediumMax()).isEqualTo(60);
      assertThat(result.getRiskThresholds().getHighMax()).isEqualTo(85);
      assertThat(result.getTaskCategories()).hasSize(2);
      assertThat(result.getPitchCategories()).hasSize(4);
      assertThat(result.getUpdatedBy()).isEqualTo(testUsername);

      verify(settingsRepository).deleteAll();
      verify(settingsRepository).save(any(OrganizationSettings.class));
    }

    @Test
    @DisplayName("Should create default task categories on reset")
    void shouldCreateDefaultTaskCategoriesOnReset() {
      // Given
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      OrganizationSettingsDTO result = settingsService.resetToDefaults(testUsername);

      // Then
      assertThat(result.getTaskCategories()).hasSize(2);
      assertThat(result.getTaskCategories().get(0).getName()).isEqualTo("PITCH_SCOPE");
      assertThat(result.getTaskCategories().get(1).getName()).isEqualTo("DEBT_IMPROVEMENT");
    }

    @Test
    @DisplayName("Should create default pitch categories on reset")
    void shouldCreateDefaultPitchCategoriesOnReset() {
      // Given
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      OrganizationSettingsDTO result = settingsService.resetToDefaults(testUsername);

      // Then
      assertThat(result.getPitchCategories()).hasSize(4);
      assertThat(result.getPitchCategories().get(0).getName()).isEqualTo("FEATURE");
      assertThat(result.getPitchCategories().get(1).getName()).isEqualTo("INFRASTRUCTURE");
      assertThat(result.getPitchCategories().get(2).getName()).isEqualTo("REFACTOR");
      assertThat(result.getPitchCategories().get(3).getName()).isEqualTo("BUG_FIX");
    }
  }

  @Nested
  @DisplayName("Default Settings Creation Tests")
  class DefaultSettingsCreationTests {

    @Test
    @DisplayName("Default settings should have correct cycle configuration")
    void defaultSettingsShouldHaveCorrectCycleConfiguration() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      OrganizationSettingsDTO result = settingsService.getSettings();

      // Then
      assertThat(result.getDefaultCycleLengthWeeks()).isEqualTo(6);
      assertThat(result.getDefaultCooldownWeeks()).isEqualTo(2);
    }

    @Test
    @DisplayName("Default settings should have correct risk thresholds")
    void defaultSettingsShouldHaveCorrectRiskThresholds() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      OrganizationSettingsDTO result = settingsService.getSettings();

      // Then
      assertThat(result.getRiskThresholds().getLowMax()).isEqualTo(30);
      assertThat(result.getRiskThresholds().getMediumMax()).isEqualTo(60);
      assertThat(result.getRiskThresholds().getHighMax()).isEqualTo(85);
    }

    @Test
    @DisplayName("Default settings should enable all features")
    void defaultSettingsShouldEnableAllFeatures() {
      // Given
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
      when(settingsRepository.save(any(OrganizationSettings.class))).thenAnswer(invocation -> {
        OrganizationSettings saved = invocation.getArgument(0);
        saved.setId(1L);
        return saved;
      });

      // When
      OrganizationSettingsDTO result = settingsService.getSettings();

      // Then
      assertThat(result.getEnableNotifications()).isTrue();
      assertThat(result.getEnableAIFeatures()).isTrue();
    }
  }
}
