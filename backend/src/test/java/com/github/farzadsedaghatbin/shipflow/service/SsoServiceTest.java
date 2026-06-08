package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.sso.CreateIdentityProviderRequest;
import com.github.farzadsedaghatbin.shipflow.dto.sso.IdentityProviderDTO;
import com.github.farzadsedaghatbin.shipflow.dto.sso.SsoInitiateResponse;
import com.github.farzadsedaghatbin.shipflow.entity.IdentityProvider;
import com.github.farzadsedaghatbin.shipflow.entity.ProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.ProvisionedVia;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.IdentityProviderRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.security.JwtTokenProvider;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link SsoService} — no Spring context, pure Mockito.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("SsoService Tests")
class SsoServiceTest {

  @Mock private IdentityProviderRepository identityProviderRepository;
  @Mock private UserRepository userRepository;
  @Mock private JwtTokenProvider jwtTokenProvider;
  @Mock private RestTemplate restTemplate;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private StringRedisTemplate stringRedisTemplate;
  @Mock private ValueOperations<String, String> valueOperations;

  private SsoService service;

  @BeforeEach
  void setUp() {
    when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

    service = new SsoService(
        identityProviderRepository,
        userRepository,
        jwtTokenProvider,
        restTemplate,
        new ObjectMapper(),
        passwordEncoder,
        Optional.of(stringRedisTemplate));

    ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:3000");
  }

  // =========================================================================
  // listProviders
  // =========================================================================

  @Nested
  @DisplayName("listProviders()")
  class ListProviders {

    @Test
    @DisplayName("returns only enabled providers")
    void returnsOnlyEnabledProviders() {
      IdentityProvider enabled = buildIdp(1L, "Okta", ProviderType.OIDC, true);
      IdentityProvider disabled = buildIdp(2L, "Azure AD", ProviderType.SAML2, false);
      when(identityProviderRepository.findAllByIsEnabledTrue()).thenReturn(List.of(enabled));

      List<IdentityProviderDTO> result = service.listProviders();

      assertThat(result).hasSize(1);
      assertThat(result.get(0).getName()).isEqualTo("Okta");
      // Public list must not expose certificate PEM
      assertThat(result.get(0).getCertificate()).isNull();
      verify(identityProviderRepository).findAllByIsEnabledTrue();
    }

    @Test
    @DisplayName("returns empty list when no providers are enabled")
    void returnsEmptyWhenNoneEnabled() {
      when(identityProviderRepository.findAllByIsEnabledTrue()).thenReturn(List.of());
      assertThat(service.listProviders()).isEmpty();
    }
  }

  // =========================================================================
  // createProvider
  // =========================================================================

  @Nested
  @DisplayName("createProvider()")
  class CreateProvider {

    @Test
    @DisplayName("persists entity and returns DTO")
    void savesAndReturnsDto() {
      CreateIdentityProviderRequest req = new CreateIdentityProviderRequest();
      req.setName("Okta");
      req.setProviderType(ProviderType.OIDC);
      req.setClientId("client-id-123");
      req.setMetadataUrl("https://okta.example.com/.well-known/openid-configuration");
      req.setIsEnabled(true);
      req.setEnforceSso(false);

      IdentityProvider saved = buildIdp(10L, "Okta", ProviderType.OIDC, true);
      saved.setClientId("client-id-123");
      when(identityProviderRepository.save(any(IdentityProvider.class))).thenReturn(saved);

      IdentityProviderDTO result = service.createProvider(req);

      assertThat(result.getId()).isEqualTo(10L);
      assertThat(result.getName()).isEqualTo("Okta");
      assertThat(result.getProviderType()).isEqualTo(ProviderType.OIDC);
      verify(identityProviderRepository).save(any(IdentityProvider.class));
    }

    @Test
    @DisplayName("defaults isEnabled and enforceSso to false when null")
    void defaultsToDisabledWhenNull() {
      CreateIdentityProviderRequest req = new CreateIdentityProviderRequest();
      req.setName("Test IdP");
      req.setProviderType(ProviderType.SAML2);
      // isEnabled and enforceSso intentionally left null

      IdentityProvider saved = buildIdp(11L, "Test IdP", ProviderType.SAML2, false);
      when(identityProviderRepository.save(any(IdentityProvider.class))).thenReturn(saved);

      IdentityProviderDTO result = service.createProvider(req);

      assertThat(result.getIsEnabled()).isFalse();
      assertThat(result.getEnforceSso()).isFalse();
    }
  }

  // =========================================================================
  // updateProvider
  // =========================================================================

  @Nested
  @DisplayName("updateProvider()")
  class UpdateProvider {

    @Test
    @DisplayName("throws ResourceNotFoundException when provider does not exist")
    void throwsWhenNotFound() {
      when(identityProviderRepository.findById(99L)).thenReturn(Optional.empty());
      CreateIdentityProviderRequest req = new CreateIdentityProviderRequest();
      req.setName("X");
      req.setProviderType(ProviderType.OIDC);

      assertThatThrownBy(() -> service.updateProvider(99L, req))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // =========================================================================
  // deleteProvider (soft-delete)
  // =========================================================================

  @Nested
  @DisplayName("deleteProvider()")
  class DeleteProvider {

    @Test
    @DisplayName("soft-deletes by setting isEnabled=false")
    void softDeletesProvider() {
      IdentityProvider idp = buildIdp(5L, "Azure AD", ProviderType.SAML2, true);
      when(identityProviderRepository.findById(5L)).thenReturn(Optional.of(idp));
      when(identityProviderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

      service.deleteProvider(5L);

      assertThat(idp.getIsEnabled()).isFalse();
      verify(identityProviderRepository).save(idp);
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when provider does not exist")
    void throwsWhenNotFound() {
      when(identityProviderRepository.findById(99L)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.deleteProvider(99L))
          .isInstanceOf(ResourceNotFoundException.class);
    }
  }

  // =========================================================================
  // initiateOidc
  // =========================================================================

  @Nested
  @DisplayName("initiateOidc()")
  class InitiateOidc {

    @Test
    @DisplayName("generates state, stores in Redis, and returns redirect URL")
    void generatesStateAndRedirectUrl() {
      IdentityProvider idp = buildIdp(1L, "Okta", ProviderType.OIDC, true);
      idp.setClientId("my-client-id");
      idp.setMetadataUrl("https://okta.example.com/oauth2/default/.well-known/openid-configuration");
      when(identityProviderRepository.findByIdAndIsEnabledTrue(1L)).thenReturn(Optional.of(idp));
      doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));

      SsoInitiateResponse response = service.initiateOidc(1L);

      assertThat(response.getRedirectUrl()).contains("https://okta.example.com/oauth2/default/authorize");
      assertThat(response.getRedirectUrl()).contains("client_id=my-client-id");
      assertThat(response.getRedirectUrl()).contains("response_type=code");
      assertThat(response.getState()).isNotBlank();

      // Verify Redis was called with the correct TTL
      verify(valueOperations).set(
          contains("sso:state:"),
          anyString(),
          eq(5L),
          eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("throws ResourceNotFoundException when provider not found or disabled")
    void throwsWhenProviderNotFound() {
      when(identityProviderRepository.findByIdAndIsEnabledTrue(99L)).thenReturn(Optional.empty());
      assertThatThrownBy(() -> service.initiateOidc(99L))
          .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("throws IllegalArgumentException when provider is SAML2 not OIDC")
    void throwsWhenNotOidcProvider() {
      IdentityProvider samlIdp = buildIdp(2L, "ADFS", ProviderType.SAML2, true);
      when(identityProviderRepository.findByIdAndIsEnabledTrue(2L)).thenReturn(Optional.of(samlIdp));

      assertThatThrownBy(() -> service.initiateOidc(2L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("not an OIDC provider");
    }
  }

  // =========================================================================
  // initiateSaml
  // =========================================================================

  @Nested
  @DisplayName("initiateSaml()")
  class InitiateSaml {

    @Test
    @DisplayName("returns SSO URL for enabled SAML2 provider")
    void returnsSsoUrl() {
      IdentityProvider idp = buildIdp(3L, "ADFS", ProviderType.SAML2, true);
      idp.setSsoUrl("https://adfs.example.com/adfs/ls");
      when(identityProviderRepository.findByIdAndIsEnabledTrue(3L)).thenReturn(Optional.of(idp));

      String ssoUrl = service.initiateSaml(3L);

      assertThat(ssoUrl).isEqualTo("https://adfs.example.com/adfs/ls");
    }

    @Test
    @DisplayName("throws IllegalArgumentException when provider is OIDC not SAML2")
    void throwsWhenNotSamlProvider() {
      IdentityProvider oidcIdp = buildIdp(4L, "Okta", ProviderType.OIDC, true);
      when(identityProviderRepository.findByIdAndIsEnabledTrue(4L)).thenReturn(Optional.of(oidcIdp));

      assertThatThrownBy(() -> service.initiateSaml(4L))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("not a SAML2 provider");
    }
  }

  // =========================================================================
  // helpers
  // =========================================================================

  private static IdentityProvider buildIdp(Long id, String name, ProviderType type, boolean enabled) {
    return IdentityProvider.builder()
        .id(id)
        .name(name)
        .providerType(type)
        .isEnabled(enabled)
        .enforceSso(false)
        .build();
  }
}
