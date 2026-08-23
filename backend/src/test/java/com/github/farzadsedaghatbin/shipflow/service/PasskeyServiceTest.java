package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyCredentialDTO;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginOptionsResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginVerifyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyRegistrationOptionsResponse;
import com.github.farzadsedaghatbin.shipflow.entity.ChallengePurpose;
import com.github.farzadsedaghatbin.shipflow.entity.PasskeyCredential;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.UserRole;
import com.github.farzadsedaghatbin.shipflow.entity.WebAuthnChallenge;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PasskeyCredentialRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WebAuthnChallengeRepository;
import com.webauthn4j.util.Base64UrlUtil;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for {@link PasskeyService}. Covers the challenge lifecycle
 * (expiry/used/not-found guards), the begin* flows (including the
 * username-enumeration handling in {@link PasskeyService#beginLogin}), and
 * self-service list/delete ownership checks.
 *
 * <p><b>Scope note:</b> the actual cryptographic attestation/assertion
 * verification inside {@code finishRegistration}/{@code finishLogin} (the
 * calls into webauthn4j's {@code WebAuthnManager.verify(...)}) is NOT
 * exercised here — that would require hand-crafting valid CBOR/COSE
 * attestation/assertion bytes signed by a real (or simulated) authenticator
 * private key, which is impractical to fake meaningfully in a unit test.
 * That verification logic is webauthn4j's own well-tested responsibility.
 * These tests instead exercise every guard clause that runs before the
 * verify() call (challenge not found / expired / used / wrong user), which is
 * this class's own logic and fully mockable.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PasskeyServiceTest {

  @Mock
  private PasskeyCredentialRepository passkeyCredentialRepository;

  @Mock
  private WebAuthnChallengeRepository webAuthnChallengeRepository;

  @Mock
  private UserRepository userRepository;

  private PasskeyService passkeyService;

  private User user;

  @BeforeEach
  void setUp() {
    passkeyService = new PasskeyService(passkeyCredentialRepository, webAuthnChallengeRepository, userRepository);
    ReflectionTestUtils.setField(passkeyService, "rpId", "localhost");
    ReflectionTestUtils.setField(passkeyService, "rpName", "ShipFlow");
    ReflectionTestUtils.setField(passkeyService, "rpOrigin", "http://localhost:3000");
    ReflectionTestUtils.setField(passkeyService, "challengeTimeoutSeconds", 300L);

    user = User.builder().id(1L).username("alice").role(UserRole.MEMBER).isActive(true).build();
  }

  // ----------------------------------------------------------------------
  // beginRegistration
  // ----------------------------------------------------------------------

  @Test
  void beginRegistration_persistsChallengeAndListsExistingCredentialsAsExcluded() {
    PasskeyCredential existing = PasskeyCredential.builder().id(10L).user(user).credentialId("existing-cred-id")
        .publicKeyCose("cose").transports("internal,hybrid").build();
    when(passkeyCredentialRepository.findByUserId(user.getId())).thenReturn(List.of(existing));

    PasskeyRegistrationOptionsResponse response = passkeyService.beginRegistration(user);

    assertThat(response.getRp().getId()).isEqualTo("localhost");
    assertThat(response.getUser().getName()).isEqualTo("alice");
    assertThat(response.getChallenge()).isNotBlank();
    assertThat(response.getExcludeCredentials()).hasSize(1);
    assertThat(response.getExcludeCredentials().get(0).getId()).isEqualTo("existing-cred-id");
    assertThat(response.getExcludeCredentials().get(0).getTransports()).containsExactly("internal", "hybrid");

    ArgumentCaptor<WebAuthnChallenge> captor = ArgumentCaptor.forClass(WebAuthnChallenge.class);
    verify(webAuthnChallengeRepository).save(captor.capture());
    WebAuthnChallenge saved = captor.getValue();
    assertThat(saved.getPurpose()).isEqualTo(ChallengePurpose.REGISTRATION);
    assertThat(saved.getUser()).isEqualTo(user);
    assertThat(saved.getUsernameHint()).isEqualTo("alice");
    assertThat(saved.getChallenge()).isEqualTo(response.getChallenge());
  }

  // ----------------------------------------------------------------------
  // finishRegistration guard clauses
  // ----------------------------------------------------------------------

  @Test
  void finishRegistration_throwsWhenNoValidChallenge() {
    when(webAuthnChallengeRepository.findValidChallengesByUserAndPurpose(eq(user), eq(ChallengePurpose.REGISTRATION),
        any())).thenReturn(List.of());

    assertThatThrownBy(() -> passkeyService.finishRegistration(user, validRegistrationRequestStub()))
        .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("No pending passkey registration");
  }

  // ----------------------------------------------------------------------
  // beginLogin — username enumeration handling
  // ----------------------------------------------------------------------

  @Test
  void beginLogin_knownUsername_returnsAllowCredentialsAndLinksChallengeToUser() {
    PasskeyCredential existing = PasskeyCredential.builder().id(10L).user(user).credentialId("cred-1")
        .publicKeyCose("cose").transports("internal").build();
    when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));
    when(passkeyCredentialRepository.findByUserId(user.getId())).thenReturn(List.of(existing));

    PasskeyLoginOptionsResponse response = passkeyService.beginLogin("alice");

    assertThat(response.getAllowCredentials()).hasSize(1);
    assertThat(response.getAllowCredentials().get(0).getId()).isEqualTo("cred-1");
    assertThat(response.getRpId()).isEqualTo("localhost");

    ArgumentCaptor<WebAuthnChallenge> captor = ArgumentCaptor.forClass(WebAuthnChallenge.class);
    verify(webAuthnChallengeRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isEqualTo(user);
    assertThat(captor.getValue().getUsernameHint()).isEqualTo("alice");
  }

  @Test
  void beginLogin_unknownUsername_returns200ShapedResponseWithEmptyAllowCredentials_notAnError() {
    when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

    PasskeyLoginOptionsResponse response = passkeyService.beginLogin("ghost");

    // No exception thrown — same response shape, just an empty credential list.
    assertThat(response.getAllowCredentials()).isEmpty();
    assertThat(response.getChallenge()).isNotBlank();
    assertThat(response.getRpId()).isEqualTo("localhost");

    ArgumentCaptor<WebAuthnChallenge> captor = ArgumentCaptor.forClass(WebAuthnChallenge.class);
    verify(webAuthnChallengeRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isNull();
    assertThat(captor.getValue().getUsernameHint()).isEqualTo("ghost");
  }

  // ----------------------------------------------------------------------
  // beginDiscoverableLogin — conditional UI / autofill (usernameless)
  // ----------------------------------------------------------------------

  @Test
  void beginDiscoverableLogin_returnsEmptyAllowCredentialsAndNoUserOrUsernameHintChallenge() {
    PasskeyLoginOptionsResponse response = passkeyService.beginDiscoverableLogin();

    assertThat(response.getAllowCredentials()).isEmpty();
    assertThat(response.getChallenge()).isNotBlank();
    assertThat(response.getRpId()).isEqualTo("localhost");

    ArgumentCaptor<WebAuthnChallenge> captor = ArgumentCaptor.forClass(WebAuthnChallenge.class);
    verify(webAuthnChallengeRepository).save(captor.capture());
    assertThat(captor.getValue().getUser()).isNull();
    assertThat(captor.getValue().getUsernameHint()).isNull();
    assertThat(captor.getValue().getPurpose()).isEqualTo(ChallengePurpose.AUTHENTICATION);
    assertThat(captor.getValue().getChallenge()).isEqualTo(response.getChallenge());
  }

  // ----------------------------------------------------------------------
  // finishLogin guard clauses
  // ----------------------------------------------------------------------

  @Test
  void finishLogin_throwsWhenNoValidChallenge() {
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().username("alice")
        .credentialId("cred-1").authenticatorData("AA").clientDataJSON("BB").signature("CC").build();
    when(webAuthnChallengeRepository.findValidChallengesByUsernameHintAndPurpose(eq("alice"),
        eq(ChallengePurpose.AUTHENTICATION), any())).thenReturn(List.of());

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void finishLogin_throwsWhenChallengeHasNoAssociatedUser() {
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(null)
        .usernameHint("ghost").purpose(ChallengePurpose.AUTHENTICATION)
        .expiryDate(LocalDateTime.now().plusMinutes(5)).used(false).build();
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().username("ghost")
        .credentialId("cred-1").authenticatorData("AA").clientDataJSON("BB").signature("CC").build();
    when(webAuthnChallengeRepository.findValidChallengesByUsernameHintAndPurpose(eq("ghost"),
        eq(ChallengePurpose.AUTHENTICATION), any())).thenReturn(List.of(challenge));

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("No passkey is registered");
  }

  @Test
  void finishLogin_throwsWhenCredentialNotFound() {
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(user)
        .usernameHint("alice").purpose(ChallengePurpose.AUTHENTICATION)
        .expiryDate(LocalDateTime.now().plusMinutes(5)).used(false).build();
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().username("alice")
        .credentialId("unknown-cred").authenticatorData("AA").clientDataJSON("BB").signature("CC").build();
    when(webAuthnChallengeRepository.findValidChallengesByUsernameHintAndPurpose(eq("alice"),
        eq(ChallengePurpose.AUTHENTICATION), any())).thenReturn(List.of(challenge));
    when(passkeyCredentialRepository.findByCredentialId("unknown-cred")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Unknown passkey credential");
  }

  @Test
  void finishLogin_throwsWhenCredentialBelongsToDifferentUser() {
    User otherUser = User.builder().id(2L).username("bob").role(UserRole.MEMBER).isActive(true).build();
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(user)
        .usernameHint("alice").purpose(ChallengePurpose.AUTHENTICATION)
        .expiryDate(LocalDateTime.now().plusMinutes(5)).used(false).build();
    PasskeyCredential credentialOwnedByBob = PasskeyCredential.builder().id(5L).user(otherUser)
        .credentialId("cred-1").publicKeyCose("cose").build();
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().username("alice")
        .credentialId("cred-1").authenticatorData("AA").clientDataJSON("BB").signature("CC").build();
    when(webAuthnChallengeRepository.findValidChallengesByUsernameHintAndPurpose(eq("alice"),
        eq(ChallengePurpose.AUTHENTICATION), any())).thenReturn(List.of(challenge));
    when(passkeyCredentialRepository.findByCredentialId("cred-1")).thenReturn(Optional.of(credentialOwnedByBob));

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("does not belong");
  }

  // ----------------------------------------------------------------------
  // finishLogin — discoverable-credential branch (conditional UI / autofill)
  // ----------------------------------------------------------------------

  @Test
  void finishLogin_discoverable_throwsWhenNoValidChallenge() {
    when(webAuthnChallengeRepository.findByChallenge(any())).thenReturn(Optional.empty());
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().credentialId("cred-1")
        .authenticatorData("AA").clientDataJSON(clientDataJSON("some-challenge")).signature("CC")
        .userHandle(userHandleFor(1L)).build();

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Invalid or expired");
  }

  @Test
  void finishLogin_discoverable_throwsWhenChallengeAlreadyUsed() {
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(null).usernameHint(null)
        .purpose(ChallengePurpose.AUTHENTICATION).expiryDate(LocalDateTime.now().plusMinutes(5)).used(true).build();
    when(webAuthnChallengeRepository.findByChallenge("chal")).thenReturn(Optional.of(challenge));
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().credentialId("cred-1")
        .authenticatorData("AA").clientDataJSON(clientDataJSON("chal")).signature("CC").userHandle(userHandleFor(1L))
        .build();

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Invalid or expired");
  }

  @Test
  void finishLogin_discoverable_throwsWhenNoUserHandle() {
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(null).usernameHint(null)
        .purpose(ChallengePurpose.AUTHENTICATION).expiryDate(LocalDateTime.now().plusMinutes(5)).used(false).build();
    when(webAuthnChallengeRepository.findByChallenge("chal")).thenReturn(Optional.of(challenge));
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().credentialId("cred-1")
        .authenticatorData("AA").clientDataJSON(clientDataJSON("chal")).signature("CC").build();

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("No passkey is registered");
  }

  @Test
  void finishLogin_discoverable_throwsWhenUserHandleUnparseable() {
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(null).usernameHint(null)
        .purpose(ChallengePurpose.AUTHENTICATION).expiryDate(LocalDateTime.now().plusMinutes(5)).used(false).build();
    when(webAuthnChallengeRepository.findByChallenge("chal")).thenReturn(Optional.of(challenge));
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().credentialId("cred-1")
        .authenticatorData("AA").clientDataJSON(clientDataJSON("chal")).signature("CC")
        .userHandle(Base64UrlUtil.encodeToString("not-a-number".getBytes(StandardCharsets.UTF_8))).build();

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("Unrecognized passkey user handle");
  }

  @Test
  void finishLogin_discoverable_throwsWhenUserHandleDoesNotResolveToAnAccount() {
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(null).usernameHint(null)
        .purpose(ChallengePurpose.AUTHENTICATION).expiryDate(LocalDateTime.now().plusMinutes(5)).used(false).build();
    when(webAuthnChallengeRepository.findByChallenge("chal")).thenReturn(Optional.of(challenge));
    when(userRepository.findById(99L)).thenReturn(Optional.empty());
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().credentialId("cred-1")
        .authenticatorData("AA").clientDataJSON(clientDataJSON("chal")).signature("CC")
        .userHandle(userHandleFor(99L)).build();

    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("No account found for this passkey");
  }

  @Test
  void finishLogin_discoverable_resolvesUserFromUserHandle_thenAppliesSameCredentialOwnershipGuard() {
    User otherUser = User.builder().id(2L).username("bob").role(UserRole.MEMBER).isActive(true).build();
    WebAuthnChallenge challenge = WebAuthnChallenge.builder().id(1L).challenge("chal").user(null).usernameHint(null)
        .purpose(ChallengePurpose.AUTHENTICATION).expiryDate(LocalDateTime.now().plusMinutes(5)).used(false).build();
    PasskeyCredential credentialOwnedByBob = PasskeyCredential.builder().id(5L).user(otherUser).credentialId("cred-1")
        .publicKeyCose("cose").build();
    when(webAuthnChallengeRepository.findByChallenge("chal")).thenReturn(Optional.of(challenge));
    when(userRepository.findById(1L)).thenReturn(Optional.of(user));
    when(passkeyCredentialRepository.findByCredentialId("cred-1")).thenReturn(Optional.of(credentialOwnedByBob));
    PasskeyLoginVerifyRequest request = PasskeyLoginVerifyRequest.builder().credentialId("cred-1")
        .authenticatorData("AA").clientDataJSON(clientDataJSON("chal")).signature("CC")
        .userHandle(userHandleFor(1L)).build();

    // user handle resolves to "alice" (id 1), but the credential belongs to "bob" — same guard
    // clause the username-first flow uses, confirming the discoverable branch reaches it correctly.
    assertThatThrownBy(() -> passkeyService.finishLogin(request)).isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("does not belong");
  }

  private static String clientDataJSON(String challenge) {
    String json = "{\"type\":\"webauthn.get\",\"challenge\":\"" + challenge
        + "\",\"origin\":\"http://localhost:3000\",\"crossOrigin\":false}";
    return Base64UrlUtil.encodeToString(json.getBytes(StandardCharsets.UTF_8));
  }

  private static String userHandleFor(long userId) {
    return Base64UrlUtil.encodeToString(Long.toString(userId).getBytes(StandardCharsets.UTF_8));
  }

  // ----------------------------------------------------------------------
  // listCredentials / deleteCredential
  // ----------------------------------------------------------------------

  @Test
  void listCredentials_mapsToDTOsWithoutExposingPublicKeyOrCredentialId() {
    PasskeyCredential cred = PasskeyCredential.builder().id(10L).user(user).credentialId("secret-cred-id")
        .publicKeyCose("secret-cose-bytes").deviceName("MacBook Touch ID").transports("internal,hybrid")
        .createdAt(LocalDateTime.now()).build();
    when(passkeyCredentialRepository.findByUserId(1L)).thenReturn(List.of(cred));

    List<PasskeyCredentialDTO> result = passkeyService.listCredentials(1L);

    assertThat(result).hasSize(1);
    PasskeyCredentialDTO dto = result.get(0);
    assertThat(dto.getId()).isEqualTo(10L);
    assertThat(dto.getDeviceName()).isEqualTo("MacBook Touch ID");
    assertThat(dto.getTransports()).containsExactly("internal", "hybrid");
  }

  @Test
  void deleteCredential_ownedByCurrentUser_deletesIt() {
    PasskeyCredential cred = PasskeyCredential.builder().id(10L).user(user).credentialId("cred-1")
        .publicKeyCose("cose").build();
    when(passkeyCredentialRepository.findById(10L)).thenReturn(Optional.of(cred));

    passkeyService.deleteCredential(1L, 10L);

    verify(passkeyCredentialRepository).delete(cred);
  }

  @Test
  void deleteCredential_notFound_throwsResourceNotFound() {
    when(passkeyCredentialRepository.findById(99L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> passkeyService.deleteCredential(1L, 99L))
        .isInstanceOf(ResourceNotFoundException.class);
    verify(passkeyCredentialRepository, never()).delete(any());
  }

  @Test
  void deleteCredential_ownedByAnotherUser_throwsAccessDenied() {
    User otherUser = User.builder().id(2L).username("bob").role(UserRole.MEMBER).isActive(true).build();
    PasskeyCredential cred = PasskeyCredential.builder().id(10L).user(otherUser).credentialId("cred-1")
        .publicKeyCose("cose").build();
    when(passkeyCredentialRepository.findById(10L)).thenReturn(Optional.of(cred));

    assertThatThrownBy(() -> passkeyService.deleteCredential(1L, 10L))
        .isInstanceOf(AccessDeniedException.class);
    verify(passkeyCredentialRepository, never()).delete(any());
  }

  private com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyRegistrationVerifyRequest
      validRegistrationRequestStub() {
    return com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyRegistrationVerifyRequest.builder()
        .deviceName("Test Device").credentialId("AA").clientDataJSON("BB").attestationObject("CC").build();
  }
}
