package com.github.farzadsedaghatbin.shipflow.config;

import java.util.Arrays;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Warns at startup when the browser-facing origin settings still hold their
 * localhost development defaults while running under the {@code prod} profile.
 *
 * <p>Both settings fail in ways that look like something else entirely:
 *
 * <ul>
 *   <li><b>CORS</b> — if the public origin is missing from
 *       {@code app.cors.allowed-origins}, every browser {@code POST} is
 *       rejected with {@code 403 Invalid CORS request}. The sign-in form
 *       reports that as "Invalid username or password", so a pure
 *       configuration problem is indistinguishable from a wrong password.
 *       {@code curl} does not reproduce it, because CORS only applies to
 *       requests carrying an {@code Origin} header.</li>
 *   <li><b>WebAuthn</b> — the Relying Party ID must equal the domain serving
 *       the page. Left as {@code localhost}, the browser refuses passkey
 *       registration before the request reaches the server, surfacing only as
 *       a generic "Failed to add passkey".</li>
 * </ul>
 *
 * <p>This validator warns rather than failing fast (unlike
 * {@link StartupSecretValidator}): running the prod profile against localhost
 * is legitimate for smoke-testing, and an API-only deployment never exercises
 * either setting. A wrong value breaks the browser experience completely but
 * is not itself a security hole, so refusing to boot would do more harm than
 * good.
 */
@Slf4j
@Component
@Profile("prod")
public class PublicOriginConfigValidator implements ApplicationRunner {

  private static final String DEFAULT_RP_ID = "localhost";

  @Value("${app.cors.allowed-origins:}")
  private String allowedOrigins;

  @Value("${app.webauthn.rp-id:}")
  private String webauthnRpId;

  @Value("${app.webauthn.rp-origin:}")
  private String webauthnRpOrigin;

  @Override
  public void run(ApplicationArguments args) {
    boolean corsLooksLocalOnly = onlyLocalOrigins(allowedOrigins);
    boolean rpIdIsDefault = DEFAULT_RP_ID.equalsIgnoreCase(webauthnRpId.trim());
    boolean rpOriginIsLocal = isLocalOrigin(webauthnRpOrigin.trim());

    if (!corsLooksLocalOnly && !rpIdIsDefault && !rpOriginIsLocal) {
      log.info("Public origin configuration validated (CORS + WebAuthn).");
      return;
    }

    log.warn("================ PUBLIC ORIGIN CONFIGURATION ================");

    if (corsLooksLocalOnly) {
      log.warn(
          "app.cors.allowed-origins contains no public origin (currently: '{}'). "
              + "Browsers will receive 403 'Invalid CORS request' on every POST, and the "
              + "sign-in form will report it as 'Invalid username or password'. "
              + "Set CORS_ALLOWED_ORIGINS to your public origin, e.g. https://example.com",
          allowedOrigins);
    }

    if (rpIdIsDefault) {
      log.warn(
          "app.webauthn.rp-id is still '{}'. Passkey registration and sign-in will be "
              + "rejected by the browser with a generic failure. Set WEBAUTHN_RP_ID to the "
              + "domain serving the frontend, e.g. example.com (no scheme, no port).",
          webauthnRpId);
    }

    if (rpOriginIsLocal) {
      log.warn(
          "app.webauthn.rp-origin is a localhost origin ('{}'). Set WEBAUTHN_RP_ORIGIN to "
              + "the exact public origin, e.g. https://example.com",
          webauthnRpOrigin);
    }

    log.warn("These are warnings, not failures — the API still works for non-browser clients.");
    log.warn("=============================================================");
  }

  /** True when every configured origin is a loopback address. */
  private boolean onlyLocalOrigins(String origins) {
    if (origins == null || origins.isBlank()) {
      return true;
    }
    List<String> entries = Arrays.stream(origins.split(","))
        .map(String::trim)
        .filter(entry -> !entry.isEmpty())
        .toList();
    return entries.isEmpty() || entries.stream().allMatch(this::isLocalOrigin);
  }

  private boolean isLocalOrigin(String origin) {
    if (origin == null || origin.isBlank()) {
      return true;
    }
    String host = origin.trim().toLowerCase().replaceFirst("^https?://", "");

    if (host.startsWith("[")) {
      // Bracketed IPv6 literal, e.g. [::1]:8080 — the port separator is the
      // colon after the closing bracket, not the colons inside the address.
      int close = host.indexOf(']');
      host = close > 0 ? host.substring(1, close) : host.substring(1);
    } else {
      host = host.replaceFirst("[:/].*$", "");
    }

    return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1")
        || host.isEmpty();
  }
}
