package com.github.farzadsedaghatbin.shipflow.service.audit;

/**
 * Turns a raw User-Agent string into something a human can scan in an audit
 * table, e.g. {@code "Chrome on macOS"} or {@code "Safari on iPhone"}.
 *
 * <p>Deliberately dependency-free and deliberately approximate. A full UA
 * parsing library carries a signature database that needs updating; this only
 * has to answer "was that my laptop, or something else?" The raw header is
 * stored alongside the summary, so nothing is lost when the guess is wrong.
 *
 * <p>Order matters throughout: Edge and Opera both claim to be Chrome, Chrome
 * claims to be Safari, and nearly everything claims to be Mozilla. Each check
 * rules out the impostors before the thing they impersonate.
 */
public final class UserAgentSummary {

  private static final int MAX_LENGTH = 160;

  private UserAgentSummary() {}

  /** @return a short "Browser on Platform" description, never null. */
  public static String summarise(String userAgent) {
    if (userAgent == null || userAgent.isBlank()) {
      return "Unknown";
    }

    String ua = userAgent.toLowerCase();
    String browser = detectBrowser(ua);
    String platform = detectPlatform(ua);

    if ("Unknown".equals(browser) && "Unknown".equals(platform)) {
      // Likely a bot, script or bespoke client — a trimmed raw value is more
      // useful here than "Unknown on Unknown".
      return userAgent.length() > MAX_LENGTH ? userAgent.substring(0, MAX_LENGTH) : userAgent;
    }
    return browser + " on " + platform;
  }

  private static String detectBrowser(String ua) {
    // Non-browser clients first: they often borrow browser tokens.
    if (ua.startsWith("curl/")) {
      return "curl";
    }
    if (ua.contains("postman")) {
      return "Postman";
    }
    if (ua.contains("python-requests") || ua.contains("httpx")) {
      return "Python script";
    }
    if (ua.contains("bot") || ua.contains("crawler") || ua.contains("spider")) {
      return "Bot";
    }
    // Edge identifies as both Chrome and Safari; check it before either.
    if (ua.contains("edg/") || ua.contains("edge/")) {
      return "Edge";
    }
    if (ua.contains("opr/") || ua.contains("opera")) {
      return "Opera";
    }
    if (ua.contains("firefox/") || ua.contains("fxios/")) {
      return "Firefox";
    }
    // Chrome identifies as Safari; check Chrome first.
    if (ua.contains("chrome/") || ua.contains("crios/")) {
      return "Chrome";
    }
    if (ua.contains("safari/")) {
      return "Safari";
    }
    return "Unknown";
  }

  private static String detectPlatform(String ua) {
    // iPadOS reports "macintosh" in desktop mode, so check iOS devices first.
    if (ua.contains("iphone")) {
      return "iPhone";
    }
    if (ua.contains("ipad")) {
      return "iPad";
    }
    // Android also contains "linux"; it must be ruled out before Linux.
    if (ua.contains("android")) {
      return "Android";
    }
    if (ua.contains("windows")) {
      return "Windows";
    }
    if (ua.contains("mac os x") || ua.contains("macintosh")) {
      return "macOS";
    }
    if (ua.contains("linux")) {
      return "Linux";
    }
    return "Unknown";
  }
}
