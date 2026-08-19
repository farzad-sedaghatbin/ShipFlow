package com.github.farzadsedaghatbin.shipflow.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for {@link ClientIpResolver}.
 *
 * <p>These guard a production incident: the malicious-header filter used to
 * read {@code X-Forwarded-For} whole, so it banned Cloudflare edge addresses
 * (blocking every visitor behind that PoP) and the loopback address Caddy
 * connects from (taking the site offline).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ClientIpResolverTest {

  private static final List<String> TRUSTED = List.of("127.0.0.1", "::1");

  @Mock
  private HttpServletRequest request;

  @Test
  @DisplayName("returns the real client, not the Cloudflare edge, from a proxy chain")
  void takesFirstHopOfForwardedChain() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.7, 162.158.217.95");

    assertEquals("203.0.113.7", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("prefers CF-Connecting-IP over the forwarded chain")
  void prefersCloudflareHeader() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getHeader("CF-Connecting-IP")).thenReturn("203.0.113.9");
    when(request.getHeader("X-Forwarded-For")).thenReturn("198.51.100.1, 172.68.159.61");

    assertEquals("203.0.113.9", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("ignores forwarding headers from an untrusted peer so bans cannot be forged")
  void ignoresHeadersFromUntrustedPeer() {
    when(request.getRemoteAddr()).thenReturn("45.148.10.125");
    when(request.getHeader("CF-Connecting-IP")).thenReturn("8.8.8.8");
    when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8");

    assertEquals("45.148.10.125", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("never returns the loopback address Caddy connects from when a chain exists")
  void doesNotReturnLoopbackWhenChainPresent() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.4");

    assertEquals("203.0.113.4", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("falls back to X-Real-IP when no forwarded chain is present")
  void fallsBackToRealIp() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.11");

    assertEquals("203.0.113.11", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("falls back to the peer address when a trusted proxy sends no headers")
  void fallsBackToRemoteAddr() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");

    assertEquals("127.0.0.1", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("treats literal \"unknown\" and blank header values as absent")
  void ignoresUnknownAndBlankValues() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getHeader("CF-Connecting-IP")).thenReturn("unknown");
    when(request.getHeader("X-Forwarded-For")).thenReturn("   ");
    when(request.getHeader("X-Real-IP")).thenReturn("203.0.113.12");

    assertEquals("203.0.113.12", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("trims surrounding whitespace from header values")
  void trimsWhitespace() {
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(request.getHeader("X-Forwarded-For")).thenReturn("  203.0.113.13 , 104.23.221.68 ");

    assertEquals("203.0.113.13", ClientIpResolver.resolve(request, TRUSTED));
  }

  @Test
  @DisplayName("ignores headers entirely when no trusted proxies are configured")
  void nullTrustedListIgnoresHeaders() {
    when(request.getRemoteAddr()).thenReturn("198.51.100.50");
    when(request.getHeader("X-Forwarded-For")).thenReturn("8.8.8.8");

    assertEquals("198.51.100.50", ClientIpResolver.resolve(request, null));
  }
}
