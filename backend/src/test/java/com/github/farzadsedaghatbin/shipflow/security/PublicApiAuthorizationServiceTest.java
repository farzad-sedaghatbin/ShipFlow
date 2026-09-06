package com.github.farzadsedaghatbin.shipflow.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/** Unit tests for {@link PublicApiAuthorizationService}. No Spring context needed. */
class PublicApiAuthorizationServiceTest {

  private final PublicApiAuthorizationService service = new PublicApiAuthorizationService();

  @BeforeEach
  void clearContext() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void tearDown() {
    SecurityContextHolder.clearContext();
  }

  private void authenticateAs(ApiKey apiKey) {
    var auth = new UsernamePasswordAuthenticationToken("api-key-caller", null, java.util.List.of());
    auth.setDetails(apiKey);
    SecurityContextHolder.getContext().setAuthentication(auth);
  }

  // ── currentApiKey / restrictedProjectIdOrNull ──────────────────────────────

  @Test
  void currentApiKey_returnsNull_whenNoAuthentication() {
    assertThat(service.currentApiKey()).isNull();
    assertThat(service.restrictedProjectIdOrNull()).isNull();
  }

  @Test
  void currentApiKey_returnsNull_whenAuthenticationDetailsIsNotAnApiKey() {
    var auth = new UsernamePasswordAuthenticationToken("some-jwt-user", null, java.util.List.of());
    auth.setDetails("not-an-api-key");
    SecurityContextHolder.getContext().setAuthentication(auth);

    assertThat(service.currentApiKey()).isNull();
    assertThat(service.restrictedProjectIdOrNull()).isNull();
  }

  @Test
  void currentApiKey_returnsTheKey_whenPresentInAuthenticationDetails() {
    ApiKey key = ApiKey.builder().id(1L).restrictedToProjectId(42L).build();
    authenticateAs(key);

    assertThat(service.currentApiKey()).isEqualTo(key);
    assertThat(service.restrictedProjectIdOrNull()).isEqualTo(42L);
  }

  @Test
  void restrictedProjectIdOrNull_isNull_forAnUnrestrictedKey() {
    ApiKey key = ApiKey.builder().id(1L).restrictedToProjectId(null).build();
    authenticateAs(key);

    assertThat(service.restrictedProjectIdOrNull()).isNull();
  }

  // ── requireProjectAccess ────────────────────────────────────────────────────

  @Test
  void requireProjectAccess_allows_whenApiKeyIsNull() {
    service.requireProjectAccess(null, 42L);
    // no exception — an internally-authenticated (non-API-key) caller is out of scope for this check
  }

  @Test
  void requireProjectAccess_allows_whenKeyIsUnrestricted() {
    ApiKey key = ApiKey.builder().id(1L).restrictedToProjectId(null).build();
    service.requireProjectAccess(key, 42L);
    service.requireProjectAccess(key, null);
    // no exception either way — an unrestricted key sees everything, matching pre-existing behavior
  }

  @Test
  void requireProjectAccess_allows_whenRestrictedProjectMatches() {
    ApiKey key = ApiKey.builder().id(1L).restrictedToProjectId(42L).build();
    service.requireProjectAccess(key, 42L);
  }

  @Test
  void requireProjectAccess_denies_whenRestrictedProjectDoesNotMatch() {
    ApiKey key = ApiKey.builder().id(1L).restrictedToProjectId(42L).build();
    assertThatThrownBy(() -> service.requireProjectAccess(key, 99L))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void requireProjectAccess_denies_whenResourceHasNoResolvableProject() {
    // A restricted key checked against an ownerless resource (resourceProjectId == null) must be
    // denied, not allowed — an ownerless resource is never safely "in scope" for a restricted key.
    ApiKey key = ApiKey.builder().id(1L).restrictedToProjectId(42L).build();
    assertThatThrownBy(() -> service.requireProjectAccess(key, null))
        .isInstanceOf(ResourceNotFoundException.class);
  }
}
