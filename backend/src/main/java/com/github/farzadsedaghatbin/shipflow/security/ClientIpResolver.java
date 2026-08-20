package com.github.farzadsedaghatbin.shipflow.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * Single source of truth for "which IP did this request actually come from".
 *
 * <p>Requests reach ShipFlow through Cloudflare, then Caddy, and each hop
 * appends itself to {@code X-Forwarded-For}. By the time a request arrives the
 * header reads {@code <real-client>, <cloudflare-edge>}, so reading it whole —
 * or taking its last entry — identifies a proxy rather than the caller.
 *
 * <p>Getting this wrong is not cosmetic. Any security decision keyed on the
 * resulting value (rate limiting, suspicious-request counting, auto-bans) then
 * targets infrastructure instead of attackers: banning a Cloudflare edge
 * address blocks every visitor routed through that PoP for the ban window, and
 * banning the loopback address Caddy connects from takes the whole site
 * offline. Both happened in production.
 *
 * <p>Proxy-supplied headers are honoured <strong>only</strong> when the direct
 * peer is a configured trusted proxy. Otherwise anyone able to reach the origin
 * directly could forge a header to evade their own ban or to get an innocent
 * third party banned in their place.
 *
 * <p>{@code CF-Connecting-IP} is preferred when present: Cloudflare writes it
 * itself and it always carries exactly one address, so it needs no parsing.
 */
final class ClientIpResolver {

  private ClientIpResolver() {}

  /**
   * @param request the inbound request
   * @param trustedProxies peer addresses whose forwarding headers may be
   *        believed; a null or non-matching list means headers are ignored
   * @return the best available client address, never null
   */
  static String resolve(HttpServletRequest request, List<String> trustedProxies) {
    String remoteAddr = request.getRemoteAddr();

    if (trustedProxies == null || !trustedProxies.contains(remoteAddr)) {
      return remoteAddr;
    }

    String cfConnectingIp = request.getHeader("CF-Connecting-IP");
    if (usable(cfConnectingIp)) {
      return cfConnectingIp.trim();
    }

    // X-Forwarded-For is a comma-separated chain, client first — take the head.
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (usable(forwardedFor)) {
      int comma = forwardedFor.indexOf(',');
      String firstHop = (comma >= 0 ? forwardedFor.substring(0, comma) : forwardedFor).trim();
      if (usable(firstHop)) {
        return firstHop;
      }
    }

    String realIp = request.getHeader("X-Real-IP");
    if (usable(realIp)) {
      return realIp.trim();
    }

    return remoteAddr;
  }

  private static boolean usable(String value) {
    return value != null && !value.isBlank() && !"unknown".equalsIgnoreCase(value.trim());
  }
}
