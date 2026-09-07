package com.github.farzadsedaghatbin.shipflow.license;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.LicenseFile;
import com.github.farzadsedaghatbin.shipflow.repository.LicenseFileRepository;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.PublicKey;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reads, verifies, and caches the currently-installed ShipFlow licence — the open-core gate
 * between the Community Edition (this public repository) and commercial features/limits. See
 * {@code CLAUDE.md} and the {@code 30-licensing.md} help guide for the full contract.
 *
 * <p>Source of truth: the single row in {@code license_file} (uploaded via {@code POST
 * /api/license}) if present, else the file at {@code app.license.file} (default {@code
 * ./config/shipflow.license}); an uploaded row always wins over the file. {@code
 * app.license.public-key} (env {@code APP_LICENSE_PUBLIC_KEY}) must be configured — a base64
 * X.509 SubjectPublicKeyInfo Ed25519 public key — or the licence can never be verified and status
 * is {@link LicenseStatus#MISSING}, logged once as a WARN per refresh.
 *
 * <p>Verification never throws past this class: a missing, malformed, or badly-signed licence
 * always resolves to {@link LicenseStatus#MISSING} with the reason logged, both at startup ({@link
 * #onApplicationEvent}) and on the {@link #dailyRefresh()} schedule — the app must always start
 * and keep running as Community Edition rather than fail closed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LicenseService implements ApplicationListener<ApplicationReadyEvent> {

  /** Days past {@code expiresAt} a licence stays in {@link LicenseStatus#GRACE} before {@link LicenseStatus#EXPIRED}. */
  public static final int GRACE_PERIOD_DAYS = 30;

  private final LicenseFileRepository licenseFileRepository;
  private final ResourceLoader resourceLoader;
  private final ObjectMapper objectMapper;

  /** Base64 X.509 SubjectPublicKeyInfo Ed25519 public key. Blank/unset = licence can never verify. */
  @Value("${app.license.public-key:}")
  private String publicKeyBase64;

  /** Filesystem (or classpath:, for tests) path to a licence file, used only when no row is uploaded. */
  @Value("${app.license.file:./config/shipflow.license}")
  private String licenseFilePath;

  private final AtomicReference<Snapshot> snapshot =
      new AtomicReference<>(Snapshot.missing("Not yet checked"));

  private record Snapshot(LicenseStatus status, LicensePayload payload, String reason) {
    static Snapshot missing(String reason) {
      return new Snapshot(LicenseStatus.MISSING, null, reason);
    }
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    refresh();
  }

  /** Re-validate the configured licence once a day, in case it crossed an expiry/grace boundary. */
  @Scheduled(cron = "0 15 3 * * *")
  public void dailyRefresh() {
    refresh();
  }

  public LicenseStatus getStatus() {
    return snapshot.get().status();
  }

  /** Why the current status is what it is — for logs/diagnostics, not part of the REST contract. */
  public String getReason() {
    return snapshot.get().reason();
  }

  public Optional<LicensePayload> currentPayload() {
    return Optional.ofNullable(snapshot.get().payload());
  }

  /** True when the current licence is VALID or GRACE and lists {@code featureName}. */
  public boolean hasFeature(String featureName) {
    Snapshot s = snapshot.get();
    return (s.status() == LicenseStatus.VALID || s.status() == LicenseStatus.GRACE) && s.payload() != null
        && s.payload().hasFeature(featureName);
  }

  /** The licensed seat count from the current payload, or 0 when there is none. */
  public int seatLimit() {
    return currentPayload().map(LicensePayload::getSeats).orElse(0);
  }

  /**
   * Validate and persist {@code content} as the new licence, replacing the single {@code
   * license_file} row in place (never appending history). Refreshes the cached status before
   * returning.
   *
   * @throws LicenseInvalidException if the content is malformed or fails signature verification
   */
  @Transactional
  public LicensePayload uploadLicense(String content) {
    LicensePayload payload = verify(content)
        .orElseThrow(() -> new LicenseInvalidException(
            "The licence file is malformed or failed signature verification."));

    LicenseFile file = licenseFileRepository.findFirstByOrderByIdDesc().orElseGet(LicenseFile::new);
    file.setContent(content);
    file.setUploadedAt(LocalDateTime.now());
    file.setUploadedBy(currentUsername());
    licenseFileRepository.save(file);

    refresh();
    log.info("Licence uploaded by '{}': licensee='{}', seats={}, expiresAt={}", file.getUploadedBy(),
        payload.getLicensee(), payload.getSeats(), payload.getExpiresAt());
    return payload;
  }

  /** Remove the uploaded licence row, reverting to {@code app.license.file} (if any) or Community Edition. */
  @Transactional
  public void removeLicense() {
    licenseFileRepository.findFirstByOrderByIdDesc().ifPresent(licenseFileRepository::delete);
    refresh();
    log.info("Licence removed by '{}'", currentUsername());
  }

  // ── Internal ─────────────────────────────────────────────────────────────

  private void refresh() {
    try {
      Optional<String> content = loadContent();
      if (content.isEmpty()) {
        snapshot.set(Snapshot.missing("No licence uploaded or configured — Community Edition"));
        return;
      }
      if (publicKeyBase64 == null || publicKeyBase64.isBlank()) {
        log.warn("A licence is configured but app.license.public-key (env APP_LICENSE_PUBLIC_KEY) is not "
            + "set — the licence cannot be verified. Running as Community Edition.");
        snapshot.set(Snapshot.missing("app.license.public-key is not configured"));
        return;
      }

      Optional<LicensePayload> payload = verify(content.get());
      if (payload.isEmpty()) {
        log.warn("The configured licence failed signature verification or is malformed. "
            + "Running as Community Edition.");
        snapshot.set(Snapshot.missing("Signature verification failed"));
        return;
      }

      snapshot.set(computeStatus(payload.get()));
    } catch (Exception e) {
      // Never let a licence problem crash startup or the daily job — see class Javadoc.
      log.error("Unexpected error refreshing licence status. Running as Community Edition.", e);
      snapshot.set(Snapshot.missing("Unexpected error: " + e.getMessage()));
    }
  }

  private Snapshot computeStatus(LicensePayload payload) {
    LocalDate today = LocalDate.now();
    LocalDate expiresAt = payload.getExpiresAt();

    if (expiresAt == null || !today.isAfter(expiresAt)) {
      return new Snapshot(LicenseStatus.VALID, payload, "Valid");
    }

    LocalDate graceEnd = expiresAt.plusDays(GRACE_PERIOD_DAYS);
    if (!today.isAfter(graceEnd)) {
      return new Snapshot(LicenseStatus.GRACE, payload,
          "Expired " + expiresAt + " — within the " + GRACE_PERIOD_DAYS + "-day grace period");
    }

    return new Snapshot(LicenseStatus.EXPIRED, payload, "Expired " + expiresAt + " — grace period over");
  }

  /** DB row wins over the configured file — see class Javadoc. */
  private Optional<String> loadContent() {
    Optional<String> uploaded = licenseFileRepository.findFirstByOrderByIdDesc().map(LicenseFile::getContent);
    if (uploaded.isPresent()) {
      return uploaded;
    }
    return loadFromConfiguredFile();
  }

  private Optional<String> loadFromConfiguredFile() {
    if (licenseFilePath == null || licenseFilePath.isBlank()) {
      return Optional.empty();
    }
    try {
      Resource resource = resourceLoader.getResource(toResourceLocation(licenseFilePath));
      if (!resource.exists()) {
        return Optional.empty();
      }
      try (InputStream in = resource.getInputStream()) {
        return Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
      }
    } catch (IOException e) {
      log.warn("Failed to read licence file at '{}': {}", licenseFilePath, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * {@link ResourceLoader} only resolves a bare path (no recognised scheme) reliably as
   * classpath-relative or filesystem-relative depending on which loader implementation is
   * injected — ambiguous across environments. Always supply an explicit scheme ourselves instead:
   * {@code classpath:}/{@code file:}/{@code http(s):} pass through unchanged; anything else (e.g.
   * the default {@code ./config/shipflow.license}) is treated as a plain OS filesystem path and
   * given an explicit {@code file:} prefix.
   */
  private static String toResourceLocation(String path) {
    return path.matches("^[a-zA-Z][a-zA-Z0-9+.-]*:.*") ? path : "file:" + path;
  }

  /**
   * Parse {@code content} as {@code {"payload": {...}, "signature": "<base64>"}}, verify the
   * Ed25519 signature over the canonical JSON bytes of the RAW {@code payload} node (not a
   * re-derived serialization — see {@link LicensePayload} class Javadoc), and only then deserialize
   * that same node into a {@link LicensePayload}. Returns empty on any malformed input or signature
   * mismatch — never throws.
   */
  private Optional<LicensePayload> verify(String content) {
    try {
      JsonNode root = objectMapper.readTree(content);
      JsonNode payloadNode = root.get("payload");
      JsonNode signatureNode = root.get("signature");
      if (payloadNode == null || payloadNode.isNull() || signatureNode == null || !signatureNode.isTextual()) {
        return Optional.empty();
      }

      byte[] canonical = CanonicalJson.canonicalBytes(payloadNode);
      byte[] signature = Base64.getDecoder().decode(signatureNode.asText());
      PublicKey publicKey = Ed25519Licenses.decodePublicKey(publicKeyBase64);

      if (!Ed25519Licenses.verify(canonical, signature, publicKey)) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.treeToValue(payloadNode, LicensePayload.class));
    } catch (Exception e) {
      log.debug("Licence verification failed: {}", e.getMessage());
      return Optional.empty();
    }
  }

  private static String currentUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
      return null;
    }
    return auth.getName();
  }
}
