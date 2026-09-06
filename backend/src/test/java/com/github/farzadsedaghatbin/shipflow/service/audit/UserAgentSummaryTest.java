package com.github.farzadsedaghatbin.shipflow.service.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UserAgentSummary}.
 *
 * <p>The interesting cases are all impersonation: Edge and Opera claim to be
 * Chrome, Chrome claims to be Safari, and Android claims to be Linux. Each
 * assertion below pins one of those orderings.
 */
class UserAgentSummaryTest {

  @Test
  @DisplayName("Chrome on macOS")
  void chromeOnMac() {
    assertEquals("Chrome on macOS", UserAgentSummary.summarise(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/140.0.0.0 Safari/537.36"));
  }

  @Test
  @DisplayName("Edge is not reported as Chrome, though it claims to be")
  void edgeNotChrome() {
    assertEquals("Edge on Windows", UserAgentSummary.summarise(
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0"));
  }

  @Test
  @DisplayName("Opera is not reported as Chrome, though it claims to be")
  void operaNotChrome() {
    assertEquals("Opera on Windows", UserAgentSummary.summarise(
        "Mozilla/5.0 (Windows NT 10.0) AppleWebKit/537.36 Chrome/140.0.0.0 Safari/537.36 OPR/115.0.0.0"));
  }

  @Test
  @DisplayName("real Safari is not reported as Chrome")
  void safariOnMac() {
    assertEquals("Safari on macOS", UserAgentSummary.summarise(
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) "
            + "Version/17.0 Safari/605.1.15"));
  }

  @Test
  @DisplayName("iPhone is not reported as macOS")
  void safariOnIphone() {
    assertEquals("Safari on iPhone", UserAgentSummary.summarise(
        "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
            + "Version/17.0 Mobile/15E148 Safari/604.1"));
  }

  @Test
  @DisplayName("Android is not reported as Linux, though its UA contains it")
  void androidNotLinux() {
    assertEquals("Chrome on Android", UserAgentSummary.summarise(
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/140.0.0.0 Mobile Safari/537.36"));
  }

  @Test
  @DisplayName("Firefox on Linux")
  void firefoxOnLinux() {
    assertEquals("Firefox on Linux",
        UserAgentSummary.summarise("Mozilla/5.0 (X11; Linux x86_64; rv:130.0) Gecko/20100101 Firefox/130.0"));
  }

  @Test
  @DisplayName("command-line and script clients are identified, not guessed at")
  void nonBrowserClients() {
    assertTrue(UserAgentSummary.summarise("curl/8.4.0").startsWith("curl"));
    assertTrue(UserAgentSummary.summarise("python-requests/2.31.0").startsWith("Python script"));
    assertTrue(UserAgentSummary.summarise("PostmanRuntime/7.36.0").startsWith("Postman"));
  }

  @Test
  @DisplayName("bots are flagged")
  void bots() {
    assertTrue(UserAgentSummary.summarise(
        "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)").startsWith("Bot"));
  }

  @Test
  @DisplayName("missing or blank User-Agent yields Unknown rather than null")
  void missingUserAgent() {
    assertEquals("Unknown", UserAgentSummary.summarise(null));
    assertEquals("Unknown", UserAgentSummary.summarise("   "));
  }

  @Test
  @DisplayName("an unrecognised agent keeps its raw value, truncated")
  void unrecognisedKeepsRawValue() {
    String weird = "SomeCustomClient/1.0";
    assertEquals(weird, UserAgentSummary.summarise(weird));
    assertEquals(160, UserAgentSummary.summarise("X".repeat(500)).length());
  }
}
