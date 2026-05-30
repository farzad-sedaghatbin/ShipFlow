package com.github.farzadsedaghatbin.shipflow.service.mcp.server;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.config.mcp.McpServerProperties;
import com.github.farzadsedaghatbin.shipflow.entity.OrganizationSettings;
import com.github.farzadsedaghatbin.shipflow.repository.OrganizationSettingsRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for {@link McpServerSettingsService} — verifies the DB-override-vs-env-default
 * resolution rule for the MCP server runtime toggle without a Spring context.
 */
@ExtendWith(MockitoExtension.class)
class McpServerSettingsServiceTest {

  @Mock private OrganizationSettingsRepository settingsRepository;

  private McpServerProperties properties;
  private McpServerSettingsService service;

  @BeforeEach
  void setUp() {
    properties = new McpServerProperties();
    service = new McpServerSettingsService(settingsRepository, properties);
  }

  private void givenNoSettingsRow() {
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
  }

  private void givenSettings(Boolean enabled, Boolean writeEnabled) {
    OrganizationSettings settings = new OrganizationSettings();
    settings.setMcpServerEnabled(enabled);
    settings.setMcpServerWriteEnabled(writeEnabled);
    when(settingsRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(settings));
  }

  // ── isEnabled ──────────────────────────────────────────────────────────────

  @Test
  void enabledFallsBackToEnvDefaultTrueWhenNoDbRow() {
    givenNoSettingsRow();
    properties.setEnabled(true);
    assertThat(service.isEnabled()).isTrue();
  }

  @Test
  void enabledFallsBackToEnvDefaultFalseWhenNoDbRow() {
    givenNoSettingsRow();
    properties.setEnabled(false);
    assertThat(service.isEnabled()).isFalse();
  }

  @Test
  void enabledFallsBackToEnvWhenDbOverrideNull() {
    givenSettings(null, null);
    properties.setEnabled(true);
    assertThat(service.isEnabled()).isTrue();
  }

  @Test
  void dbOverrideTrueWinsOverEnvFalse() {
    givenSettings(true, null);
    properties.setEnabled(false);
    assertThat(service.isEnabled()).isTrue();
  }

  @Test
  void dbOverrideFalseWinsOverEnvTrue() {
    givenSettings(false, null);
    properties.setEnabled(true);
    assertThat(service.isEnabled()).isFalse();
  }

  // ── isWriteEnabled ───────────────────────────────────────────────────────────

  @Test
  void writeDisabledWhenServerDisabled() {
    // Server off via DB override, even though write flag + env say on.
    givenSettings(false, true);
    properties.setEnabled(true);
    properties.setWriteEnabled(true);
    assertThat(service.isWriteEnabled()).isFalse();
  }

  @Test
  void writeEnabledViaDbOverride() {
    givenSettings(true, true);
    properties.setWriteEnabled(false); // env says off
    assertThat(service.isWriteEnabled()).isTrue();
  }

  @Test
  void writeFallsBackToEnvTrueWhenNoWriteOverride() {
    givenSettings(true, null);
    properties.setWriteEnabled(true);
    assertThat(service.isWriteEnabled()).isTrue();
  }

  @Test
  void writeFallsBackToEnvFalseWhenNoWriteOverride() {
    givenSettings(true, null);
    properties.setWriteEnabled(false);
    assertThat(service.isWriteEnabled()).isFalse();
  }
}
