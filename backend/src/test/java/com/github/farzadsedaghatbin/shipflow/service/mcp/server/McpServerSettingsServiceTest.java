package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.config.mcp.McpServerProperties;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link McpServerSettingsService}. Verifies that the effective MCP server state is
 * resolved as: DB override (when non-null) wins, otherwise the environment-variable default from
 * {@link McpServerProperties}. Write mode additionally requires the server to be enabled.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("McpServerSettingsService Tests")
class McpServerSettingsServiceTest {

  @Mock private OrganizationSettingsRepository settingsRepository;

  private McpServerProperties properties;
  private McpServerSettingsService service;

  @BeforeEach
  void setUp() {
    properties = new McpServerProperties();
    service = new McpServerSettingsService(settingsRepository, properties);
  }

  private OrganizationSettings settings(Boolean enabled, Boolean writeEnabled) {
    return OrganizationSettings.builder()
        .id(1L)
        .mcpServerEnabled(enabled)
        .mcpServerWriteEnabled(writeEnabled)
        .build();
  }

  @Nested
  @DisplayName("isEnabled()")
  class IsEnabled {

    @Test
    @DisplayName("falls back to env default (false) when no settings row exists")
    void noRowUsesEnvDefaultFalse() {
      properties.setEnabled(false);
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

      assertThat(service.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("falls back to env default (true) when no settings row exists")
    void noRowUsesEnvDefaultTrue() {
      properties.setEnabled(true);
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

      assertThat(service.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("falls back to env default when DB value is null")
    void nullDbValueUsesEnvDefault() {
      properties.setEnabled(true);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(null, null)));

      assertThat(service.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("DB override true wins over env default false")
    void dbTrueOverridesEnvFalse() {
      properties.setEnabled(false);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(true, null)));

      assertThat(service.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("DB override false wins over env default true")
    void dbFalseOverridesEnvTrue() {
      properties.setEnabled(true);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(false, null)));

      assertThat(service.isEnabled()).isFalse();
    }
  }

  @Nested
  @DisplayName("isWriteEnabled()")
  class IsWriteEnabled {

    @Test
    @DisplayName("is false when the server is disabled, regardless of write flag")
    void disabledServerForcesWriteFalse() {
      properties.setEnabled(false);
      properties.setWriteEnabled(true);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(false, true)));

      assertThat(service.isWriteEnabled()).isFalse();
    }

    @Test
    @DisplayName("uses env write default when enabled and DB write value is null")
    void enabledNullWriteUsesEnvDefault() {
      properties.setEnabled(true);
      properties.setWriteEnabled(true);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(true, null)));

      assertThat(service.isWriteEnabled()).isTrue();
    }

    @Test
    @DisplayName("env write default false applies when enabled and DB write value is null")
    void enabledNullWriteEnvFalse() {
      properties.setEnabled(true);
      properties.setWriteEnabled(false);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(true, null)));

      assertThat(service.isWriteEnabled()).isFalse();
    }

    @Test
    @DisplayName("DB write override true wins over env write default false (server enabled)")
    void dbWriteTrueOverridesEnvFalse() {
      properties.setEnabled(true);
      properties.setWriteEnabled(false);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(true, true)));

      assertThat(service.isWriteEnabled()).isTrue();
    }

    @Test
    @DisplayName("DB write override false wins over env write default true (server enabled)")
    void dbWriteFalseOverridesEnvTrue() {
      properties.setEnabled(true);
      properties.setWriteEnabled(true);
      when(settingsRepository.findFirstByOrderByIdAsc())
          .thenReturn(Optional.of(settings(true, false)));

      assertThat(service.isWriteEnabled()).isFalse();
    }

    @Test
    @DisplayName("uses both env defaults when no settings row exists and server enabled by env")
    void noRowUsesEnvDefaults() {
      properties.setEnabled(true);
      properties.setWriteEnabled(true);
      when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());

      assertThat(service.isWriteEnabled()).isTrue();
    }
  }
}
