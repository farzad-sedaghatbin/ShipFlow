package com.github.farzadsedaghatbin.shipflow.license;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.farzadsedaghatbin.shipflow.entity.LicenseFile;
import com.github.farzadsedaghatbin.shipflow.repository.LicenseFileRepository;
import java.security.KeyPair;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LicenseServiceTest {

  @Mock private LicenseFileRepository licenseFileRepository;

  private LicenseService licenseService;
  private KeyPair keyPair;
  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    keyPair = Ed25519Licenses.generateKeyPair();
    objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());

    licenseService = new LicenseService(licenseFileRepository, new DefaultResourceLoader(), objectMapper);
    ReflectionTestUtils.setField(licenseService, "publicKeyBase64", Ed25519Licenses.encodePublicKey(keyPair.getPublic()));
    ReflectionTestUtils.setField(licenseService, "licenseFilePath", "");

    when(licenseFileRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.empty());
  }

  private String signedLicense(LicensePayload payload, KeyPair signingKeyPair) throws Exception {
    var payloadNode = objectMapper.valueToTree(payload);
    byte[] canonical = CanonicalJson.canonicalBytes(payloadNode);
    byte[] signature = Ed25519Licenses.sign(canonical, signingKeyPair.getPrivate());
    ObjectNode root = objectMapper.createObjectNode();
    root.set("payload", payloadNode);
    root.put("signature", Base64.getEncoder().encodeToString(signature));
    return objectMapper.writeValueAsString(root);
  }

  private LicensePayload.LicensePayloadBuilder validPayloadBuilder() {
    return LicensePayload.builder()
        .id(UUID.randomUUID().toString())
        .licensee("Acme Corp")
        .edition("COMMERCIAL")
        .seats(50)
        .features(List.of("sso", "audit-export"))
        .issuedAt(LocalDate.now().minusDays(10))
        .expiresAt(LocalDate.now().plusYears(1))
        .supportUntil(LocalDate.now().plusYears(1));
  }

  // ── Status computation ──────────────────────────────────────────────────

  @Test
  void status_NoLicenceConfigured_IsMissing() {
    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
    assertThat(licenseService.currentPayload()).isEmpty();
    assertThat(licenseService.seatLimit()).isZero();
    assertThat(licenseService.hasFeature("sso")).isFalse();
  }

  @Test
  void status_PublicKeyNotConfigured_IsMissing() throws Exception {
    ReflectionTestUtils.setField(licenseService, "publicKeyBase64", "");
    String content = signedLicense(validPayloadBuilder().build(), keyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
    assertThat(licenseService.getReason()).contains("public-key");
  }

  @Test
  void status_ValidSignature_NotExpired_IsValid() throws Exception {
    LicensePayload payload = validPayloadBuilder().build();
    String content = signedLicense(payload, keyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.VALID);
    assertThat(licenseService.seatLimit()).isEqualTo(50);
    assertThat(licenseService.hasFeature("sso")).isTrue();
    assertThat(licenseService.hasFeature("ldap")).isFalse();
    assertThat(licenseService.currentPayload()).isPresent();
  }

  @Test
  void status_WrongSigningKey_IsMissing() throws Exception {
    KeyPair otherKeyPair = Ed25519Licenses.generateKeyPair();
    String content = signedLicense(validPayloadBuilder().build(), otherKeyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
  }

  @Test
  void status_MalformedJson_IsMissing() {
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content("not json").build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
  }

  @Test
  void status_MissingSignatureField_IsMissing() {
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content("{\"payload\":{\"seats\":5}}").build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
  }

  @Test
  void status_TamperedPayloadAfterSigning_IsMissing() throws Exception {
    LicensePayload payload = validPayloadBuilder().build();
    String content = signedLicense(payload, keyPair);

    ObjectNode root = (ObjectNode) objectMapper.readTree(content);
    ((ObjectNode) root.get("payload")).put("seats", 999999);
    String tampered = objectMapper.writeValueAsString(root);

    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(tampered).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
  }

  @Test
  void status_ExpiredWithinGracePeriod_IsGrace() throws Exception {
    LicensePayload payload = validPayloadBuilder().expiresAt(LocalDate.now().minusDays(5)).build();
    String content = signedLicense(payload, keyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.GRACE);
    assertThat(licenseService.hasFeature("sso")).isTrue(); // GRACE behaves like VALID for features
  }

  @Test
  void status_ExpiredPastGracePeriod_IsExpired() throws Exception {
    LicensePayload payload =
        validPayloadBuilder().expiresAt(LocalDate.now().minusDays(LicenseService.GRACE_PERIOD_DAYS + 1)).build();
    String content = signedLicense(payload, keyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.EXPIRED);
    assertThat(licenseService.hasFeature("sso")).isFalse();
  }

  @Test
  void status_ExpiresExactlyToday_IsStillValid() throws Exception {
    LicensePayload payload = validPayloadBuilder().expiresAt(LocalDate.now()).build();
    String content = signedLicense(payload, keyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.VALID);
  }

  @Test
  void dailyRefresh_AlsoUpdatesStatus() throws Exception {
    String content = signedLicense(validPayloadBuilder().build(), keyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.dailyRefresh();

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.VALID);
  }

  // ── Upload / remove ─────────────────────────────────────────────────────

  @Test
  void uploadLicense_ValidContent_PersistsAndRefreshes() throws Exception {
    String content = signedLicense(validPayloadBuilder().build(), keyPair);
    // uploadLicense() calls findFirstByOrderByIdDesc() once for itself, then refresh() calls it
    // again to re-read the just-saved row — a plain mock has no real persistence, so simulate a
    // save-then-read-back with a small in-memory holder rather than a static thenReturn.
    var savedRow = new java.util.concurrent.atomic.AtomicReference<LicenseFile>();
    when(licenseFileRepository.save(any(LicenseFile.class))).thenAnswer(inv -> {
      LicenseFile f = inv.getArgument(0);
      savedRow.set(f);
      return f;
    });
    when(licenseFileRepository.findFirstByOrderByIdDesc()).thenAnswer(inv -> Optional.ofNullable(savedRow.get()));

    LicensePayload result = licenseService.uploadLicense(content);

    assertThat(result.getLicensee()).isEqualTo("Acme Corp");
    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.VALID);
    verify(licenseFileRepository).save(any(LicenseFile.class));
  }

  @Test
  void uploadLicense_ReplacesExistingRow_RatherThanAppending() throws Exception {
    LicenseFile existing = LicenseFile.builder().id(9L).content("old").build();
    when(licenseFileRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(existing));
    when(licenseFileRepository.save(any(LicenseFile.class))).thenAnswer(inv -> inv.getArgument(0));

    String content = signedLicense(validPayloadBuilder().build(), keyPair);
    licenseService.uploadLicense(content);

    var captor = org.mockito.ArgumentCaptor.forClass(LicenseFile.class);
    verify(licenseFileRepository).save(captor.capture());
    assertThat(captor.getValue().getId()).isEqualTo(9L); // same row, not a new one
    assertThat(captor.getValue().getContent()).isEqualTo(content);
  }

  @Test
  void uploadLicense_InvalidContent_ThrowsLicenseInvalidException_DoesNotPersist() {
    assertThatThrownBy(() -> licenseService.uploadLicense("garbage")).isInstanceOf(LicenseInvalidException.class);

    verify(licenseFileRepository, never()).save(any());
  }

  @Test
  void removeLicense_DeletesRowAndRefreshes() throws Exception {
    String content = signedLicense(validPayloadBuilder().build(), keyPair);
    LicenseFile existing = LicenseFile.builder().id(7L).content(content).build();
    when(licenseFileRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(existing));
    licenseService.onApplicationEvent(null);
    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.VALID);

    when(licenseFileRepository.findFirstByOrderByIdDesc()).thenReturn(Optional.of(existing), Optional.empty());

    licenseService.removeLicense();

    verify(licenseFileRepository).delete(existing);
    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
  }

  // ── File-based configuration ────────────────────────────────────────────

  @Test
  void loadFromConfiguredFile_UsedOnlyWhenNoUploadedRow() {
    // The committed test fixture is signed with a DIFFERENT keypair than this test's own, so it
    // resolves to MISSING here — but the reason confirms the file WAS read and parsed (wrong
    // signature) rather than silently skipped (which would say "no licence configured").
    ReflectionTestUtils.setField(licenseService, "licenseFilePath", "classpath:license/test-license.json");

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
    assertThat(licenseService.getReason()).isEqualTo("Signature verification failed");
  }

  @Test
  void loadFromConfiguredFile_NonExistentPath_IsMissing_NeverThrows() {
    ReflectionTestUtils.setField(licenseService, "licenseFilePath", "classpath:license/does-not-exist.json");

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.MISSING);
  }

  @Test
  void uploadedRow_TakesPriorityOverConfiguredFile() throws Exception {
    ReflectionTestUtils.setField(licenseService, "licenseFilePath", "classpath:license/does-not-exist.json");
    String content = signedLicense(validPayloadBuilder().licensee("From DB row").build(), keyPair);
    when(licenseFileRepository.findFirstByOrderByIdDesc())
        .thenReturn(Optional.of(LicenseFile.builder().id(1L).content(content).build()));

    licenseService.onApplicationEvent(null);

    assertThat(licenseService.getStatus()).isEqualTo(LicenseStatus.VALID);
    assertThat(licenseService.currentPayload()).get().extracting(LicensePayload::getLicensee).isEqualTo("From DB row");
  }
}
