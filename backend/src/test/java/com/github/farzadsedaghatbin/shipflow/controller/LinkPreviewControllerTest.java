package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.config.SocialCrawlerFilter;
import com.github.farzadsedaghatbin.shipflow.service.LinkPreviewService;
import com.github.farzadsedaghatbin.shipflow.service.LinkPreviewService.LinkPreview;
import java.util.Optional;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.ForwardedHeaderFilter;

/**
 * Standalone (no Spring context) tests for the Open Graph pages behind link previews.
 *
 * <p>The interesting behaviour here is host derivation: these URLs used to be hardcoded to
 * {@code https://shipflow.dev}, so every self-hosted instance advertised the public demo's host.
 * What each preview <em>says</em> comes from {@link LinkPreviewService} and is tested there.
 */
@DisplayName("Link Preview Controller")
class LinkPreviewControllerTest {

  private static final LinkPreview TASK = new LinkPreview("Instant transfer", "Task · Backlog · Urgent");

  private MockMvc mockMvc;
  private LinkPreviewService linkPreviewService;

  @BeforeEach
  void setUp() {
    linkPreviewService = Mockito.mock(LinkPreviewService.class);
    mockMvc = MockMvcBuilders.standaloneSetup(new LinkPreviewController(linkPreviewService)).build();
  }

  /**
   * An HTTPS request as a real container reports it. MockMvc otherwise pairs an {@code https} URI
   * with port 80, which no deployment ever does and which would show up as {@code https://host:80}
   * in the assertions below.
   */
  private MockHttpServletRequestBuilder previewRequest(String path, String host) {
    return get(path).secure(true).with(request -> {
      // secure(true) alone only flips the secure flag — the scheme follows the URI, so set it.
      request.setScheme("https");
      request.setServerName(host);
      request.setServerPort(443);
      return request;
    });
  }

  @Test
  @DisplayName("task preview advertises the host actually being browsed, not shipflow.dev")
  void taskPreviewUsesRequestHost() throws Exception {
    when(linkPreviewService.forTask(7L)).thenReturn(Optional.of(TASK));

    mockMvc.perform(previewRequest("/preview/task/7", "shipflow.acme.internal"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:url\" content=\"https://shipflow.acme.internal/backlog/7\"/>")))
        .andExpect(content().string(Matchers.containsString(
            "content=\"https://shipflow.acme.internal/android-chrome-512x512.png\"")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("shipflow.dev"))));
  }

  @Test
  @DisplayName("behind a TLS-terminating proxy the preview advertises https, no port")
  void behindProxyUsesForwardedScheme() throws Exception {
    when(linkPreviewService.forTask(7L)).thenReturn(Optional.of(TASK));

    // The shape Cloudflare (or nginx, or an ingress) sends the container: plain HTTP on an
    // internal port, public scheme in X-Forwarded-Proto. ForwardedHeaderFilter is what
    // `server.forward-headers-strategy=framework` installs in the prod profile; without it the
    // app builds http:// URLs, which is what cost the previews in the first place.
    MockMvc behindProxy = MockMvcBuilders
        .standaloneSetup(new LinkPreviewController(linkPreviewService))
        .addFilters(new ForwardedHeaderFilter())
        .build();

    behindProxy.perform(get("/preview/task/7")
            .with(request -> {
              request.setScheme("http");
              request.setServerName("shipflow.dev");
              request.setServerPort(8080);
              return request;
            })
            .header("Host", "shipflow.dev")
            .header("X-Forwarded-Proto", "https"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:url\" content=\"https://shipflow.dev/backlog/7\"/>")))
        .andExpect(content().string(Matchers.not(Matchers.containsString("http://"))));
  }

  // The page HTML-escapes everything it prints, so the "·" separator in a summary is emitted as the
  // entity &middot; — which crawlers decode back to "·". The assertions below expect that escaped form.
  @Test
  @DisplayName("the card shows exactly the title and summary the lookup service returned")
  void previewShowsTheServiceTitleAndSummary() throws Exception {
    when(linkPreviewService.forTask(7L)).thenReturn(Optional.of(TASK));

    mockMvc.perform(previewRequest("/preview/task/7", "host"))
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:title\" content=\"Instant transfer — ShipFlow\"/>")))
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:description\" content=\"Task &middot; Backlog &middot; Urgent\"/>")));
  }

  @Test
  @DisplayName("a direct /preview hit bounces the visitor on to the SPA")
  void directHitKeepsBounceScript() throws Exception {
    when(linkPreviewService.forTask(7L)).thenReturn(Optional.of(TASK));

    mockMvc.perform(previewRequest("/preview/task/7", "host"))
        .andExpect(content().string(Matchers.containsString(
            "window.location.replace('/backlog/7')")));
  }

  @Test
  @DisplayName("served in place at the shared URL, the bounce script is omitted")
  void inPlaceOmitsBounceScript() throws Exception {
    when(linkPreviewService.forTask(7L)).thenReturn(Optional.of(TASK));

    // What SocialCrawlerFilter's forward looks like from the controller's side. Keeping the
    // bounce here would reload the very URL being previewed, forever, for any crawler that
    // executes JavaScript.
    mockMvc.perform(previewRequest("/preview/task/7", "host")
            .requestAttr(SocialCrawlerFilter.IN_PLACE_ATTRIBUTE, Boolean.TRUE))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.not(Matchers.containsString("window.location.replace"))))
        .andExpect(content().string(Matchers.containsString("og:title")));
  }

  @Test
  @DisplayName("bug report preview links the bug report URL")
  void bugPreview() throws Exception {
    when(linkPreviewService.forBug(9L)).thenReturn(Optional.of(
        new LinkPreview("Login loops", "Bug report · Trivial · Open")));

    mockMvc.perform(previewRequest("/preview/bug/9", "host"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:url\" content=\"https://host/qa/bug-reports/9\"/>")))
        .andExpect(content().string(Matchers.containsString("Login loops")))
        .andExpect(content().string(Matchers.containsString("Bug report &middot; Trivial &middot; Open")));
  }

  @Test
  @DisplayName("pitch preview links the pitch URL")
  void pitchPreview() throws Exception {
    when(linkPreviewService.forPitch(3L)).thenReturn(Optional.of(
        new LinkPreview("Faster onboarding", "Pitch · In Progress")));

    mockMvc.perform(previewRequest("/preview/pitch/3", "host"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:url\" content=\"https://host/pitches/3\"/>")))
        .andExpect(content().string(Matchers.containsString("Pitch &middot; In Progress")));
  }

  @Test
  @DisplayName("cycle preview links the cycle URL")
  void cyclePreview() throws Exception {
    when(linkPreviewService.forCycle(12L)).thenReturn(Optional.of(
        new LinkPreview("Cycle 12", "Cycle: 4 pitches · 2026-08-01 – 2026-08-28")));

    mockMvc.perform(previewRequest("/preview/cycle/12", "host"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:url\" content=\"https://host/cycles/12\"/>")))
        .andExpect(content().string(Matchers.containsString("4 pitches")));
  }

  @Test
  @DisplayName("an unknown or deleted entity returns 404 with generic branding")
  void unknownEntityIsNotFound() throws Exception {
    when(linkPreviewService.forTask(anyLong())).thenReturn(Optional.empty());

    mockMvc.perform(previewRequest("/preview/task/999", "host"))
        .andExpect(status().isNotFound())
        .andExpect(content().string(Matchers.containsString("ShipFlow")))
        // 404 stays at the entity's own URL so the SPA can render its usual not-found state.
        .andExpect(content().string(Matchers.containsString("https://host/backlog/999")));
  }

  @Test
  @DisplayName("a missing bug report returns 404 at its own URL")
  void unknownBugIsNotFound() throws Exception {
    when(linkPreviewService.forBug(anyLong())).thenReturn(Optional.empty());

    mockMvc.perform(previewRequest("/preview/bug/999", "host"))
        .andExpect(status().isNotFound())
        .andExpect(content().string(Matchers.containsString("https://host/qa/bug-reports/999")));
  }

  @Test
  @DisplayName("titles and summaries are HTML-escaped")
  void escapesHtml() throws Exception {
    when(linkPreviewService.forTask(7L)).thenReturn(Optional.of(
        new LinkPreview("<script>alert(1)</script>", "\"quoted\"")));

    mockMvc.perform(previewRequest("/preview/task/7", "host"))
        .andExpect(content().string(Matchers.not(Matchers.containsString("<script>alert(1)"))))
        .andExpect(content().string(Matchers.containsString("&lt;script&gt;")));
  }
}
