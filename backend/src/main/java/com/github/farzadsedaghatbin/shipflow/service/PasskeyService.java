package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.dto.passkey.CredentialDescriptor;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyCredentialDTO;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginOptionsResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyLoginVerifyRequest;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyRegistrationOptionsResponse;
import com.github.farzadsedaghatbin.shipflow.dto.passkey.PasskeyRegistrationVerifyRequest;
import com.github.farzadsedaghatbin.shipflow.entity.ChallengePurpose;
import com.github.farzadsedaghatbin.shipflow.entity.PasskeyCredential;
import com.github.farzadsedaghatbin.shipflow.entity.User;
import com.github.farzadsedaghatbin.shipflow.entity.WebAuthnChallenge;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.PasskeyCredentialRepository;
import com.github.farzadsedaghatbin.shipflow.repository.UserRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WebAuthnChallengeRepository;
import com.webauthn4j.WebAuthnManager;
import com.webauthn4j.authenticator.Authenticator;
import com.webauthn4j.authenticator.AuthenticatorImpl;
import com.webauthn4j.converter.AttestedCredentialDataConverter;
import com.webauthn4j.converter.util.ObjectConverter;
import com.webauthn4j.data.AuthenticationParameters;
import com.webauthn4j.data.AuthenticationData;
import com.webauthn4j.data.AuthenticationRequest;
import com.webauthn4j.data.PublicKeyCredentialParameters;
import com.webauthn4j.data.PublicKeyCredentialType;
import com.webauthn4j.data.RegistrationData;
import com.webauthn4j.data.RegistrationParameters;
import com.webauthn4j.data.RegistrationRequest;
import com.webauthn4j.data.attestation.authenticator.AttestedCredentialData;
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier;
import com.webauthn4j.data.client.Origin;
import com.webauthn4j.data.client.challenge.DefaultChallenge;
import com.webauthn4j.server.ServerProperty;
import com.webauthn4j.util.Base64UrlUtil;
import com.webauthn4j.verifier.exception.VerificationException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * WebAuthn/passkey registration and authentication ceremonies, backed by
 * webauthn4j's {@link WebAuthnManager} for all cryptographic
 * attestation/assertion verification — no signature math is hand-rolled here.
 *
 * <h2>Binary field encoding convention</h2>
 *
 * Every WebAuthn field that is binary per spec (challenge, credentialId,
 * clientDataJSON, attestationObject, authenticatorData, signature,
 * userHandle) is exchanged with the frontend as a base64url string. The
 * frontend's WebAuthn helper is expected to base64url-decode/encode when
 * calling {@code navigator.credentials.create()}/{@code get()}.
 *
 * <h2>Design choice — username enumeration in {@link #beginLogin}</h2>
 *
 * This is a username-first flow (the user types a username before the
 * platform authenticator prompt, rather than a fully "discoverable
 * credential" / usernameless flow). When the supplied username does not
 * resolve to any user, {@link #beginLogin} still returns HTTP 200 with the
 * same {@link PasskeyLoginOptionsResponse} shape — just with an empty
 * {@code allowCredentials} list — rather than a 404. This avoids a
 * status-code/error-message oracle. It does not eliminate every side channel
 * (an empty {@code allowCredentials} list is itself observable, same as it
 * would be for a real user who simply has no passkeys registered), but that
 * is inherent to a username-first design and is the same trade-off most
 * production WebAuthn implementations accept. A fully discoverable-credential
 * (resident key / usernameless) flow would remove this, at the cost of
 * additional client and RP complexity — out of scope for this session per
 * the task's design directive.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasskeyService {

  private static final long CHALLENGE_LENGTH_BYTES = 32;
  private static final long CEREMONY_TIMEOUT_MILLIS = 60_000L;

  private final PasskeyCredentialRepository passkeyCredentialRepository;
  private final WebAuthnChallengeRepository webAuthnChallengeRepository;
  private final UserRepository userRepository;

  @Value("${app.webauthn.rp-id:localhost}")
  private String rpId;

  @Value("${app.webauthn.rp-name:ShipFlow}")
  private String rpName;

  @Value("${app.webauthn.rp-origin:http://localhost:3000}")
  private String rpOrigin;

  @Value("${app.webauthn.challenge-timeout-seconds:300}")
  private long challengeTimeoutSeconds;

  private final SecureRandom secureRandom = new SecureRandom();
  private final ObjectConverter objectConverter = new ObjectConverter();
  private final AttestedCredentialDataConverter attestedCredentialDataConverter =
      new AttestedCredentialDataConverter(objectConverter);

  // Non-strict: skips attestation trust-anchor verification against FIDO metadata,
  // which is the appropriate posture for "none"/self attestation from platform
  // authenticators (Touch ID, Windows Hello, etc.) — this is the standard setup
  // used by RPs that don't operate their own FIDO Metadata Service integration.
  private final WebAuthnManager webAuthnManager = WebAuthnManager.createNonStrictWebAuthnManager();

  private static final List<PublicKeyCredentialParameters> SUPPORTED_ALGORITHMS = List.of(
      new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256),
      new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.RS256),
      new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.EdDSA));

  // ----------------------------------------------------------------------
  // Registration (self-service, current authenticated user)
  // ----------------------------------------------------------------------

  @Transactional
  public PasskeyRegistrationOptionsResponse beginRegistration(User user) {
    String challenge = newChallenge();

    WebAuthnChallenge entity = WebAuthnChallenge.builder().challenge(challenge).user(user)
        .usernameHint(user.getUsername()).purpose(ChallengePurpose.REGISTRATION)
        .expiryDate(LocalDateTime.now().plusSeconds(challengeTimeoutSeconds)).used(false).build();
    webAuthnChallengeRepository.save(entity);

    List<CredentialDescriptor> excludeCredentials = passkeyCredentialRepository.findByUserId(user.getId())
        .stream()
        .map(c -> CredentialDescriptor.builder().id(c.getCredentialId()).transports(splitTransports(c.getTransports()))
            .build())
        .collect(Collectors.toList());

    String displayName = user.getPerson() != null && user.getPerson().getName() != null
        ? user.getPerson().getName() : user.getUsername();

    return PasskeyRegistrationOptionsResponse.builder()
        .rp(PasskeyRegistrationOptionsResponse.RelyingParty.builder().id(rpId).name(rpName).build())
        .user(PasskeyRegistrationOptionsResponse.UserInfo.builder()
            .id(Base64UrlUtil.encodeToString(user.getId().toString().getBytes(StandardCharsets.UTF_8)))
            .name(user.getUsername()).displayName(displayName).build())
        .challenge(challenge)
        .pubKeyCredParams(List.of(
            PasskeyRegistrationOptionsResponse.PubKeyCredParam.builder().alg(COSEAlgorithmIdentifier.ES256.getValue())
                .build(),
            PasskeyRegistrationOptionsResponse.PubKeyCredParam.builder().alg(COSEAlgorithmIdentifier.RS256.getValue())
                .build(),
            PasskeyRegistrationOptionsResponse.PubKeyCredParam.builder().alg(COSEAlgorithmIdentifier.EdDSA.getValue())
                .build()))
        .timeout(CEREMONY_TIMEOUT_MILLIS)
        .excludeCredentials(excludeCredentials)
        .authenticatorSelection(PasskeyRegistrationOptionsResponse.AuthenticatorSelection.builder()
            .residentKey("discouraged").userVerification("preferred").build())
        .attestation("none")
        .build();
  }

  @Transactional
  public PasskeyCredentialDTO finishRegistration(User user, PasskeyRegistrationVerifyRequest request) {
    WebAuthnChallenge challenge = webAuthnChallengeRepository
        .findValidChallengesByUserAndPurpose(user, ChallengePurpose.REGISTRATION, LocalDateTime.now())
        .stream()
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(
            "No pending passkey registration was found for this account, or it has expired. Please retry."));

    byte[] attestationObjectBytes = Base64UrlUtil.decode(request.getAttestationObject());
    byte[] clientDataJSONBytes = Base64UrlUtil.decode(request.getClientDataJSON());

    RegistrationRequest registrationRequest = new RegistrationRequest(attestationObjectBytes, clientDataJSONBytes);
    ServerProperty serverProperty =
        new ServerProperty(Origin.create(rpOrigin), rpId, new DefaultChallenge(challenge.getChallenge()));
    RegistrationParameters registrationParameters =
        new RegistrationParameters(serverProperty, SUPPORTED_ALGORITHMS, false, true);

    RegistrationData registrationData;
    try {
      registrationData = webAuthnManager.verify(registrationRequest, registrationParameters);
    } catch (VerificationException e) {
      log.warn("Passkey registration verification failed for user '{}': {}", user.getUsername(), e.getMessage());
      throw new IllegalArgumentException("Passkey registration could not be verified: " + e.getMessage(), e);
    }

    AttestedCredentialData attestedCredentialData =
        registrationData.getAttestationObject().getAuthenticatorData().getAttestedCredentialData();
    if (attestedCredentialData == null) {
      throw new IllegalArgumentException("Authenticator did not return attested credential data.");
    }

    String credentialId = Base64UrlUtil.encodeToString(attestedCredentialData.getCredentialId());
    if (passkeyCredentialRepository.existsByCredentialId(credentialId)) {
      throw new IllegalArgumentException("This authenticator is already registered.");
    }

    String publicKeyCose =
        Base64.getEncoder().encodeToString(attestedCredentialDataConverter.convert(attestedCredentialData));
    long signCount = registrationData.getAttestationObject().getAuthenticatorData().getSignCount();
    String attestationType = registrationData.getAttestationObject().getAttestationStatement().getFormat();

    Set<String> transports = registrationData.getTransports() == null ? Set.of()
        : registrationData.getTransports().stream().map(t -> t.getValue()).collect(Collectors.toSet());
    if (transports.isEmpty() && request.getTransports() != null) {
      transports = Set.copyOf(request.getTransports());
    }

    String deviceName = request.getDeviceName() != null && !request.getDeviceName().isBlank()
        ? request.getDeviceName() : "Passkey";

    PasskeyCredential credential = PasskeyCredential.builder().user(user).credentialId(credentialId)
        .publicKeyCose(publicKeyCose).signCount(signCount).attestationType(attestationType)
        .transports(String.join(",", transports)).deviceName(deviceName).build();
    credential = passkeyCredentialRepository.save(credential);

    challenge.setUsed(true);
    webAuthnChallengeRepository.save(challenge);

    log.info("Passkey registered for user '{}' (credential db id {})", user.getUsername(), credential.getId());
    return toDTO(credential);
  }

  // ----------------------------------------------------------------------
  // Authentication (username-first login, unauthenticated caller)
  // ----------------------------------------------------------------------

  @Transactional
  public PasskeyLoginOptionsResponse beginLogin(String username) {
    String challengeValue = newChallenge();
    User user = userRepository.findByUsername(username).orElse(null);

    List<CredentialDescriptor> allowCredentials = user == null ? List.of()
        : passkeyCredentialRepository.findByUserId(user.getId()).stream()
            .map(c -> CredentialDescriptor.builder().id(c.getCredentialId())
                .transports(splitTransports(c.getTransports())).build())
            .collect(Collectors.toList());

    WebAuthnChallenge challenge = WebAuthnChallenge.builder().challenge(challengeValue).user(user)
        .usernameHint(username).purpose(ChallengePurpose.AUTHENTICATION)
        .expiryDate(LocalDateTime.now().plusSeconds(challengeTimeoutSeconds)).used(false).build();
    webAuthnChallengeRepository.save(challenge);

    return PasskeyLoginOptionsResponse.builder().challenge(challengeValue).rpId(rpId)
        .timeout(CEREMONY_TIMEOUT_MILLIS).allowCredentials(allowCredentials).userVerification("preferred").build();
  }

  /** Returns the username to mint a JWT for, once the assertion has been cryptographically verified. */
  @Transactional
  public String finishLogin(PasskeyLoginVerifyRequest request) {
    WebAuthnChallenge challenge = webAuthnChallengeRepository
        .findValidChallengesByUsernameHintAndPurpose(request.getUsername(), ChallengePurpose.AUTHENTICATION,
            LocalDateTime.now())
        .stream()
        .findFirst()
        .orElseThrow(() -> new BadCredentialsException("Invalid or expired passkey login challenge."));

    User user = challenge.getUser();
    if (user == null) {
      // The username never resolved to an account when the challenge was issued.
      throw new BadCredentialsException("No passkey is registered for this account.");
    }

    PasskeyCredential credential = passkeyCredentialRepository.findByCredentialId(request.getCredentialId())
        .orElseThrow(() -> new BadCredentialsException("Unknown passkey credential."));
    if (!credential.getUser().getId().equals(user.getId())) {
      throw new BadCredentialsException("Passkey does not belong to this account.");
    }

    byte[] credentialIdBytes = Base64UrlUtil.decode(request.getCredentialId());
    byte[] userHandleBytes = request.getUserHandle() != null ? Base64UrlUtil.decode(request.getUserHandle()) : null;
    byte[] authenticatorDataBytes = Base64UrlUtil.decode(request.getAuthenticatorData());
    byte[] clientDataJSONBytes = Base64UrlUtil.decode(request.getClientDataJSON());
    byte[] signatureBytes = Base64UrlUtil.decode(request.getSignature());

    AuthenticationRequest authenticationRequest = new AuthenticationRequest(credentialIdBytes, userHandleBytes,
        authenticatorDataBytes, clientDataJSONBytes, signatureBytes);

    AttestedCredentialData attestedCredentialData =
        attestedCredentialDataConverter.convert(Base64.getDecoder().decode(credential.getPublicKeyCose()));
    // AttestationStatement is intentionally null here — it is only consulted during
    // registration verification, not authentication (assertion) verification.
    Authenticator authenticator = new AuthenticatorImpl(attestedCredentialData, null, credential.getSignCount());

    ServerProperty serverProperty =
        new ServerProperty(Origin.create(rpOrigin), rpId, new DefaultChallenge(challenge.getChallenge()));
    AuthenticationParameters authenticationParameters =
        new AuthenticationParameters(serverProperty, authenticator, false, true);

    AuthenticationData authenticationData;
    try {
      authenticationData = webAuthnManager.verify(authenticationRequest, authenticationParameters);
    } catch (VerificationException e) {
      log.warn("Passkey authentication verification failed for user '{}': {}", user.getUsername(), e.getMessage());
      throw new BadCredentialsException("Passkey authentication could not be verified.", e);
    }

    // webauthn4j already rejected a non-increasing counter above (MaliciousCounterValueException);
    // persist the new counter value and last-used timestamp ourselves — the library only updates
    // the transient Authenticator instance we passed in, not our JPA entity.
    credential.setSignCount(authenticationData.getAuthenticatorData().getSignCount());
    credential.setLastUsedAt(LocalDateTime.now());
    passkeyCredentialRepository.save(credential);

    challenge.setUsed(true);
    webAuthnChallengeRepository.save(challenge);

    log.info("Passkey login succeeded for user '{}' (credential db id {})", user.getUsername(), credential.getId());
    return user.getUsername();
  }

  // ----------------------------------------------------------------------
  // Self-service management
  // ----------------------------------------------------------------------

  @Transactional(readOnly = true)
  public List<PasskeyCredentialDTO> listCredentials(Long userId) {
    return passkeyCredentialRepository.findByUserId(userId).stream().map(this::toDTO).collect(Collectors.toList());
  }

  @Transactional
  public void deleteCredential(Long userId, Long credentialId) {
    PasskeyCredential credential = passkeyCredentialRepository.findById(credentialId)
        .orElseThrow(() -> new ResourceNotFoundException("Passkey not found with id: " + credentialId));
    if (!credential.getUser().getId().equals(userId)) {
      throw new AccessDeniedException("This passkey does not belong to the current user.");
    }
    passkeyCredentialRepository.delete(credential);
  }

  // ----------------------------------------------------------------------
  // Helpers
  // ----------------------------------------------------------------------

  private String newChallenge() {
    byte[] bytes = new byte[(int) CHALLENGE_LENGTH_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64UrlUtil.encodeToString(bytes);
  }

  private List<String> splitTransports(String transports) {
    if (transports == null || transports.isBlank()) {
      return List.of();
    }
    return List.of(transports.split(","));
  }

  private PasskeyCredentialDTO toDTO(PasskeyCredential credential) {
    return PasskeyCredentialDTO.builder().id(credential.getId()).deviceName(credential.getDeviceName())
        .createdAt(credential.getCreatedAt()).lastUsedAt(credential.getLastUsedAt())
        .transports(splitTransports(credential.getTransports())).build();
  }
}
