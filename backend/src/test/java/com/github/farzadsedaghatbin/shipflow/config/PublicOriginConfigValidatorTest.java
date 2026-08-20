package com.github.farzadsedaghatbin.shipflow.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PublicOriginConfigValidator}.
 *
 * <p>Guards a real production incident: browser sign-in returned
 * {@code 403 Invalid CORS request} for months because docker-compose supplied
 * a localhost-only default, and the UI reported it as "Invalid username or
 * password". Passkey registration failed the same way via {@code rp-id}.
 */
class PublicOriginConfigValidatorTest {

  private PublicOriginConfigValidator validatorWith(String origins, String rpId, String rpOrigin) {
    PublicOriginConfigValidator v = new PublicOriginConfigValidator();
    set(v, "allowedOrigins", origins);
    set(v, "webauthnRpId", rpId);
    set(v, "webauthnRpOrigin", rpOrigin);
    return v;
  }

  private static void set(Object target, String field, String value) {
    try {
      Field f = PublicOriginConfigValidator.class.getDeclaredField(field);
      f.setAccessible(true);
      f.set(target, value);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  private static boolean invoke(PublicOriginConfigValidator v, String method, String arg) {
    try {
      Method m = PublicOriginConfigValidator.class.getDeclaredMethod(method, String.class);
      m.setAccessible(true);
      return (boolean) m.invoke(v, arg);
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }

  @Test
  @DisplayName("flags the localhost-only CORS default that broke browser sign-in")
  void detectsLocalhostOnlyCors() {
    PublicOriginConfigValidator v = validatorWith("http://localhost:*,http://127.0.0.1:*", "x", "x");
    assertTrue(invoke(v, "onlyLocalOrigins", "http://localhost:*,http://127.0.0.1:*"));
  }

  @Test
  @DisplayName("accepts a CORS list containing a public origin")
  void acceptsPublicOrigin() {
    PublicOriginConfigValidator v = validatorWith("x", "x", "x");
    assertFalse(invoke(v, "onlyLocalOrigins", "https://example.com,http://localhost:*"));
  }

  @Test
  @DisplayName("treats blank or missing CORS config as local-only")
  void blankCorsIsLocalOnly() {
    PublicOriginConfigValidator v = validatorWith("x", "x", "x");
    assertTrue(invoke(v, "onlyLocalOrigins", ""));
    assertTrue(invoke(v, "onlyLocalOrigins", null));
  }

  @Test
  @DisplayName("recognises loopback origins in every common spelling")
  void recognisesLoopbackForms() {
    PublicOriginConfigValidator v = validatorWith("x", "x", "x");
    assertTrue(invoke(v, "isLocalOrigin", "http://localhost:3000"));
    assertTrue(invoke(v, "isLocalOrigin", "https://127.0.0.1"));
    assertTrue(invoke(v, "isLocalOrigin", "localhost"));
    assertTrue(invoke(v, "isLocalOrigin", "http://[::1]:8080"));
    assertFalse(invoke(v, "isLocalOrigin", "https://example.com"));
    assertFalse(invoke(v, "isLocalOrigin", "https://shipflow.dev"));
  }

  @Test
  @DisplayName("does not mistake a domain merely containing 'localhost' for loopback")
  void doesNotMatchLookalikeDomain() {
    PublicOriginConfigValidator v = validatorWith("x", "x", "x");
    assertFalse(invoke(v, "isLocalOrigin", "https://localhost.example.com"));
  }

  @Test
  @DisplayName("warns but never throws on a fully misconfigured deployment")
  void warnsWithoutFailing() {
    PublicOriginConfigValidator v =
        validatorWith("http://localhost:*", "localhost", "http://localhost:3000");
    assertDoesNotThrow(() -> v.run(null));
  }

  @Test
  @DisplayName("stays silent when everything is configured correctly")
  void silentWhenCorrect() {
    PublicOriginConfigValidator v =
        validatorWith("https://example.com", "example.com", "https://example.com");
    assertDoesNotThrow(() -> v.run(null));
  }
}
