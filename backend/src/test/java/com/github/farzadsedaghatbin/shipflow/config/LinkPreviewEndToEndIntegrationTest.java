package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.service.LinkPreviewService;
import com.github.farzadsedaghatbin.shipflow.service.LinkPreviewService.LinkPreview;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

/**
 * The shared URL, fetched the way WhatsApp fetches it, through the real server: security chain,
 * {@link SocialCrawlerFilter}'s forward, and {@code LinkPreviewController}. MockMvc cannot cover
 * this — it records a forward but never dispatches it — so each piece passing its own unit test
 * did not prove that a pasted link actually comes back as a card.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Link previews, end to end")
class LinkPreviewEndToEndIntegrationTest {

  private static final String WHATSAPP = "WhatsApp/2.23.20.0 A";

  @LocalServerPort
  private int port;

  @MockBean
  private LinkPreviewService linkPreviewService;

  @BeforeEach
  void setUp() {
    when(linkPreviewService.forTask(7L)).thenReturn(Optional.of(new LinkPreview("Instant transfer", "Task")));
    when(linkPreviewService.forBug("9")).thenReturn(Optional.of(new LinkPreview("Login loops", "Bug report")));
    when(linkPreviewService.forBug("BUG-150")).thenReturn(Optional.of(new LinkPreview("Login loops", "Bug report")));
    when(linkPreviewService.forWikiSpace(1L)).thenReturn(Optional.of(new LinkPreview("Engineering", "Wiki space")));
    when(linkPreviewService.forWikiPage(1L, 8L)).thenReturn(Optional.of(new LinkPreview("Runbook", "Wiki page")));
    when(linkPreviewService.forInitiative(4L)).thenReturn(Optional.of(new LinkPreview("Q4 growth", "Initiative")));
    when(linkPreviewService.forRoadmap()).thenReturn(new LinkPreview("Roadmap", "Roadmap"));
  }

  private HttpResponse<String> fetch(String path, String userAgent) throws Exception {
    HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
        .header("User-Agent", userAgent).build();
    return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
  }

  private void assertCard(String path, String title) throws Exception {
    HttpResponse<String> response = fetch(path, WHATSAPP);
    assertThat(response.statusCode()).as(path).isEqualTo(200);
    assertThat(response.body()).as(path)
        .contains("<meta property=\"og:title\" content=\"" + title + " — ShipFlow\"/>")
        .doesNotContain("window.location.replace");
  }

  @Test
  @DisplayName("every shareable item URL comes back as its own card")
  void everySharedUrlIsACard() throws Exception {
    assertCard("/backlog/7", "Instant transfer");
    assertCard("/tasks/7", "Instant transfer");
    assertCard("/qa/bug-reports/BUG-150", "Login loops");
    assertCard("/qa/bug-reports/9", "Login loops");
    assertCard("/bugs/9", "Login loops");
    assertCard("/wiki/1", "Engineering");
    assertCard("/wiki/1/8", "Runbook");
    assertCard("/initiatives/4", "Q4 growth");
    assertCard("/roadmap", "Roadmap");
  }

  @Test
  @DisplayName("a real browser at the same URL is not given the crawler card")
  void browsersAreNotIntercepted() throws Exception {
    HttpResponse<String> response = fetch("/qa/bug-reports/BUG-150",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/126 Safari/537.36");

    assertThat(response.body()).doesNotContain("Login loops — ShipFlow");
  }
}
