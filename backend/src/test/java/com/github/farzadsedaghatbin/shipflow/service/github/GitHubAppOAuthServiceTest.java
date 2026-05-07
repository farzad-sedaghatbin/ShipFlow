package com.github.farzadsedaghatbin.shipflow.service.github;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.github.*;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubAppInstallation;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubAppInstallationRepository;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubRepositoryRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for GitHubAppOAuthService Tests OAuth flow, installation
 * management, and bulk repository sync
 */
@ExtendWith(MockitoExtension.class)
class GitHubAppOAuthServiceTest {

  @Mock
  private GitHubAppInstallationRepository installationRepository;

  @Mock
  private GitHubRepositoryRepository repositoryRepository;

  @Mock
  private RestTemplate restTemplate;

  private GitHubAppOAuthService service;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    service = new GitHubAppOAuthService(installationRepository, repositoryRepository, objectMapper, restTemplate);

    // Set configuration values using reflection
    ReflectionTestUtils.setField(service, "appId", "123456");
    ReflectionTestUtils.setField(service, "appName", "test-shipflow");
    ReflectionTestUtils.setField(service, "configuredBaseUrl", "https://shipflow.example.com");
  }

  @Test
  void isAppConfigured_withAllConfig_returnsTrue() {
    // Given - configured in setUp with appId and appName
    ReflectionTestUtils.setField(service, "privateKey",
        "-----BEGIN RSA PRIVATE KEY-----\ntest\n-----END RSA PRIVATE KEY-----");

    // When
    boolean result = service.isAppConfigured();

    // Then
    assertThat(result).isTrue();
  }

  @Test
  void isAppConfigured_withMissingAppId_returnsFalse() {
    // Given
    ReflectionTestUtils.setField(service, "appId", "");

    // When
    boolean result = service.isAppConfigured();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void isAppConfigured_withMissingPrivateKey_returnsFalse() {
    // Given
    ReflectionTestUtils.setField(service, "privateKey", "");

    // When
    boolean result = service.isAppConfigured();

    // Then
    assertThat(result).isFalse();
  }

  @Test
  void generateAuthorizationUrl_returnsValidUrl() {
    // Given
    GitHubOAuthInitRequest request = new GitHubOAuthInitRequest();
    request.setRedirectUrl("https://shipflow.example.com/settings");
    request.setBaseUrl("https://shipflow.example.com");

    // When
    GitHubOAuthUrlResponse response = service.generateAuthorizationUrl(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.getAuthorizationUrl()).contains("github.com/apps/test-shipflow/installations/new");
    assertThat(response.getState()).isNotNull();
    assertThat(response.getCallbackUrl()).isEqualTo("https://shipflow.example.com/api/github/app/callback");
    assertThat(response.getInstructions()).isNotEmpty();
  }

  @Test
  void generateAuthorizationUrl_withSuggestedOrg_includesTargetId() {
    // Given
    GitHubOAuthInitRequest request = new GitHubOAuthInitRequest();
    request.setSuggestedOrganization("myorg");
    request.setBaseUrl("https://shipflow.example.com");

    // When
    GitHubOAuthUrlResponse response = service.generateAuthorizationUrl(request);

    // Then
    assertThat(response.getAuthorizationUrl()).contains("suggested_target_id=myorg");
  }

  @Test
  void generateAuthorizationUrl_withCustomState_usesProvidedState() {
    // Given
    GitHubOAuthInitRequest request = new GitHubOAuthInitRequest();
    request.setState("custom-state-123");
    request.setBaseUrl("https://shipflow.example.com");

    // When
    GitHubOAuthUrlResponse response = service.generateAuthorizationUrl(request);

    // Then
    assertThat(response.getState()).isEqualTo("custom-state-123");
    assertThat(response.getAuthorizationUrl()).contains("state=custom-state-123");
  }

  @Test
  void processCallback_withInvalidState_returnsError() {
    // Given
    Long installationId = 12345L;
    String invalidState = "invalid-state";

    // When
    GitHubOAuthCallbackResult result = service.processCallback(installationId, invalidState, "install");

    // Then
    assertThat(result.isSuccess()).isFalse();
    assertThat(result.getError()).isEqualTo("invalid_state");
    assertThat(result.getErrorDescription()).contains("invalid or expired");
  }

  @Test
  void getAllInstallations_returnsActiveInstallations() {
    // Given
    GitHubAppInstallation installation1 = createTestInstallation(1L, "org1", "Organization");
    GitHubAppInstallation installation2 = createTestInstallation(2L, "org2", "Organization");
    when(installationRepository.findByIsActiveTrue()).thenReturn(Arrays.asList(installation1, installation2));

    // When
    List<GitHubAppInstallationDTO> result = service.getAllInstallations();

    // Then
    assertThat(result).hasSize(2);
    assertThat(result.get(0).getAccountLogin()).isEqualTo("org1");
    assertThat(result.get(1).getAccountLogin()).isEqualTo("org2");
  }

  @Test
  void getInstallation_existingId_returnsInstallation() {
    // Given
    GitHubAppInstallation installation = createTestInstallation(1L, "testorg", "Organization");
    when(installationRepository.findById(1L)).thenReturn(Optional.of(installation));

    // When
    Optional<GitHubAppInstallationDTO> result = service.getInstallation(1L);

    // Then
    assertThat(result).isPresent();
    assertThat(result.get().getAccountLogin()).isEqualTo("testorg");
    assertThat(result.get().getAccountType()).isEqualTo("Organization");
  }

  @Test
  void getInstallation_nonExistingId_returnsEmpty() {
    // Given
    when(installationRepository.findById(999L)).thenReturn(Optional.empty());

    // When
    Optional<GitHubAppInstallationDTO> result = service.getInstallation(999L);

    // Then
    assertThat(result).isEmpty();
  }

  @Test
  void removeInstallation_deactivatesInstallation() {
    // Given
    GitHubAppInstallation installation = createTestInstallation(1L, "testorg", "Organization");
    when(installationRepository.findById(1L)).thenReturn(Optional.of(installation));

    // When
    service.removeInstallation(1L);

    // Then
    assertThat(installation.getIsActive()).isFalse();
    verify(installationRepository).save(installation);
  }

  @Test
  void getConfigurationStatus_returnsCorrectStatus() {
    // Given
    ReflectionTestUtils.setField(service, "privateKey", "test-key");
    ReflectionTestUtils.setField(service, "clientId", "test-client-id");
    ReflectionTestUtils.setField(service, "webhookSecret", "test-secret");

    when(installationRepository.findByIsActiveTrue())
        .thenReturn(Arrays.asList(createTestInstallation(1L, "org1", "Organization")));
    when(installationRepository.countTotalRepositories()).thenReturn(50L);

    // When
    Map<String, Object> status = service.getConfigurationStatus();

    // Then
    assertThat(status.get("configured")).isEqualTo(true);
    assertThat(status.get("appName")).isEqualTo("test-shipflow");
    assertThat(status.get("totalInstallations")).isEqualTo(1);
    assertThat(status.get("totalRepositories")).isEqualTo(50L);
    assertThat(status.get("installationUrl")).isEqualTo("https://github.com/apps/test-shipflow/installations/new");
  }

  @Test
  void syncAllRepositories_withNoInstallations_returnsEmptyResult() {
    // Given
    when(installationRepository.findByIsActiveTrue()).thenReturn(List.of());

    // When
    GitHubBulkSyncResultDTO result = service.syncAllRepositories();

    // Then
    assertThat(result.getInstallationsProcessed()).isEqualTo(0);
    assertThat(result.getRepositoriesDiscovered()).isEqualTo(0);
    assertThat(result.isSuccess()).isTrue();
  }

  @Test
  void mapToDTO_mapsAllFields() {
    // Given
    GitHubAppInstallation installation = createTestInstallation(1L, "testorg", "Organization");
    installation.setRepositorySelection("all");
    installation.setWebhooksConfigured(true);
    installation.setRepositoryCount(25);
    installation.setPermissions("{\"contents\":\"read\",\"pull_requests\":\"write\"}");

    when(installationRepository.findById(1L)).thenReturn(Optional.of(installation));

    // When
    Optional<GitHubAppInstallationDTO> result = service.getInstallation(1L);

    // Then
    assertThat(result).isPresent();
    GitHubAppInstallationDTO dto = result.get();
    assertThat(dto.getAccountLogin()).isEqualTo("testorg");
    assertThat(dto.getAccountType()).isEqualTo("Organization");
    assertThat(dto.getRepositorySelection()).isEqualTo("all");
    assertThat(dto.getWebhooksConfigured()).isTrue();
    assertThat(dto.getRepositoryCount()).isEqualTo(25);
    assertThat(dto.getPermissions()).isNotNull();
    assertThat(dto.getPermissions().getContents()).isEqualTo("read");
    assertThat(dto.getPermissions().getPullRequests()).isEqualTo("write");
  }

  // Helper methods

  private GitHubAppInstallation createTestInstallation(Long id, String accountLogin, String accountType) {
    GitHubAppInstallation installation = new GitHubAppInstallation();
    installation.setId(id);
    installation.setInstallationId(1000L + id);
    installation.setAccountLogin(accountLogin);
    installation.setAccountType(accountType);
    installation.setAccountId(id * 100);
    installation.setRepositorySelection("all");
    installation.setIsActive(true);
    installation.setWebhooksConfigured(true);
    installation.setCreatedAt(LocalDateTime.now());
    installation.setUpdatedAt(LocalDateTime.now());
    return installation;
  }
}
