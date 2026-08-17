package com.github.farzadsedaghatbin.shipflow.controller;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.farzadsedaghatbin.shipflow.config.SocialCrawlerFilter;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.time.LocalDate;
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
 * <p>The interesting behaviour is host derivation: these URLs used to be hardcoded to
 * {@code https://shipflow.dev}, so every self-hosted instance advertised the public demo's host.
 */
@DisplayName("Link Preview Controller")
class LinkPreviewControllerTest {

  private MockMvc mockMvc;
  private TaskRepository taskRepository;
  private PitchRepository pitchRepository;
  private CycleRepository cycleRepository;

  @BeforeEach
  void setUp() {
    taskRepository = Mockito.mock(TaskRepository.class);
    pitchRepository = Mockito.mock(PitchRepository.class);
    cycleRepository = Mockito.mock(CycleRepository.class);
    mockMvc = MockMvcBuilders
        .standaloneSetup(new LinkPreviewController(pitchRepository, taskRepository, cycleRepository))
        .build();
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
    when(taskRepository.findById(anyLong())).thenReturn(Optional.of(
        Task.builder().title("Instant transfer").description("POST /transfers").build()));

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
    when(taskRepository.findById(anyLong())).thenReturn(Optional.of(
        Task.builder().title("Instant transfer").build()));

    // The shape Cloudflare (or nginx, or an ingress) sends the container: plain HTTP on an
    // internal port, public scheme in X-Forwarded-Proto. ForwardedHeaderFilter is what
    // `server.forward-headers-strategy=framework` installs in the prod profile; without it the
    // app builds http:// URLs, which is what cost the previews in the first place.
    MockMvc behindProxy = MockMvcBuilders
        .standaloneSetup(new LinkPreviewController(pitchRepository, taskRepository, cycleRepository))
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

  @Test
  @DisplayName("task preview keeps the crawler-visible title and description")
  void taskPreviewExposesTitleAndDescription() throws Exception {
    when(taskRepository.findById(anyLong())).thenReturn(Optional.of(
        Task.builder().title("Instant transfer").description("**Bold** and `code`").build()));

    mockMvc.perform(previewRequest("/preview/task/7", "host"))
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:title\" content=\"Instant transfer — ShipFlow\"/>")))
        // Markdown is stripped so the description reads cleanly in a preview card
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:description\" content=\"Bold and code\"/>")));
  }

  @Test
  @DisplayName("a direct /preview hit bounces the visitor on to the SPA")
  void directHitKeepsBounceScript() throws Exception {
    when(taskRepository.findById(anyLong())).thenReturn(Optional.of(
        Task.builder().title("Instant transfer").build()));

    mockMvc.perform(previewRequest("/preview/task/7", "host"))
        .andExpect(content().string(Matchers.containsString(
            "window.location.replace('/backlog/7')")));
  }

  @Test
  @DisplayName("served in place at the shared URL, the bounce script is omitted")
  void inPlaceOmitsBounceScript() throws Exception {
    when(taskRepository.findById(anyLong())).thenReturn(Optional.of(
        Task.builder().title("Instant transfer").build()));

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
  @DisplayName("pitch preview prefers the problem statement and links the pitch URL")
  void pitchPreview() throws Exception {
    when(pitchRepository.findById(anyLong())).thenReturn(Optional.of(
        Pitch.builder().title("Faster onboarding").problemStatement("Signup takes 9 steps")
            .description("ignored when a problem statement exists").build()));

    mockMvc.perform(previewRequest("/preview/pitch/3", "host"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:url\" content=\"https://host/pitches/3\"/>")))
        .andExpect(content().string(Matchers.containsString("Signup takes 9 steps")));
  }

  @Test
  @DisplayName("cycle preview summarises the dates and links the cycle URL")
  void cyclePreview() throws Exception {
    when(cycleRepository.findById(anyLong())).thenReturn(Optional.of(
        Cycle.builder().name("Cycle 12").startDate(LocalDate.of(2026, 8, 1))
            .endDate(LocalDate.of(2026, 8, 28)).build()));
    when(pitchRepository.countByCycleIdNotDeleted(12L)).thenReturn(4L);

    mockMvc.perform(previewRequest("/preview/cycle/12", "host"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString(
            "<meta property=\"og:url\" content=\"https://host/cycles/12\"/>")))
        .andExpect(content().string(Matchers.containsString("4 pitches")))
        .andExpect(content().string(Matchers.containsString("2026-08-01")));
  }

  @Test
  @DisplayName("cycle preview counts pitches by query, never through the lazy collection")
  void cyclePreviewDoesNotTouchLazyCollection() throws Exception {
    // Regression: cyclePreview used to read cycle.getPitches().size(). The collection is LAZY and
    // the controller runs outside a transaction, so that threw LazyInitializationException and
    // every cycle link previewed as a 500 — reproduced against the live demo before this fix.
    // Mimic a detached entity by making the getter blow up the way Hibernate's proxy does.
    Cycle detached = Mockito.spy(Cycle.builder().name("Cycle 12")
        .startDate(LocalDate.of(2026, 8, 1)).endDate(LocalDate.of(2026, 8, 28)).build());
    Mockito.when(detached.getPitches())
        .thenThrow(new org.hibernate.LazyInitializationException("no Session"));
    when(cycleRepository.findById(anyLong())).thenReturn(Optional.of(detached));
    when(pitchRepository.countByCycleIdNotDeleted(12L)).thenReturn(1L);

    mockMvc.perform(previewRequest("/preview/cycle/12", "host"))
        .andExpect(status().isOk())
        .andExpect(content().string(Matchers.containsString("1 pitch ")));
  }

  @Test
  @DisplayName("an unknown entity returns 404 with generic branding")
  void unknownEntityIsNotFound() throws Exception {
    when(taskRepository.findById(anyLong())).thenReturn(Optional.empty());

    mockMvc.perform(previewRequest("/preview/task/999", "host"))
        .andExpect(status().isNotFound())
        .andExpect(content().string(Matchers.containsString("ShipFlow")));
  }

  @Test
  @DisplayName("titles and descriptions are HTML-escaped")
  void escapesHtml() throws Exception {
    when(taskRepository.findById(anyLong())).thenReturn(Optional.of(
        Task.builder().title("<script>alert(1)</script>").description("\"quoted\"").build()));

    mockMvc.perform(previewRequest("/preview/task/7", "host"))
        .andExpect(content().string(Matchers.not(Matchers.containsString("<script>alert(1)"))))
        .andExpect(content().string(Matchers.containsString("&lt;script&gt;")));
  }
}
