package com.github.farzadsedaghatbin.shipflow.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Tests for the crawler hand-off behind social link previews.
 *
 * <p>The regression these lock down: the filter used to answer crawlers with a redirect to
 * {@code /preview/**}. Tomcat absolutises a relative redirect using the request's own scheme, which
 * behind a TLS-terminating proxy is {@code http} — so a crawler that asked for an HTTPS URL was sent
 * to an HTTP one, and the ones that refuse a protocol downgrade (Twitterbot, WhatsApp) showed no
 * preview at all. It must forward instead, keeping the OG page at the shared URL.
 */
@DisplayName("Social Crawler Filter")
class SocialCrawlerFilterTest {

  private final SocialCrawlerFilter filter = new SocialCrawlerFilter();

  private MockHttpServletRequest crawlerRequest(String uri, String userAgent) {
    int q = uri.indexOf('?');
    MockHttpServletRequest request = new MockHttpServletRequest("GET", q < 0 ? uri : uri.substring(0, q));
    if (q >= 0) {
      for (String pair : uri.substring(q + 1).split("&")) {
        String[] kv = pair.split("=", 2);
        request.addParameter(kv[0], kv.length > 1 ? kv[1] : "");
      }
    }
    request.setScheme("https");
    request.setServerName("shipflow.dev");
    request.setSecure(true);
    if (userAgent != null) {
      request.addHeader("User-Agent", userAgent);
    }
    return request;
  }

  @Test
  @DisplayName("forwards a crawler to the preview handler instead of redirecting")
  void forwardsCrawlerWithoutRedirecting() throws Exception {
    MockHttpServletRequest request = crawlerRequest("/backlog/42", "Twitterbot/1.0");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getForwardedUrl()).isEqualTo("/preview/task/42");
    // No redirect: no 3xx, no Location header for a crawler to (refuse to) follow
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getRedirectedUrl()).isNull();
    assertThat(response.getHeader("Location")).isNull();
    assertThat(request.getAttribute(SocialCrawlerFilter.IN_PLACE_ATTRIBUTE)).isEqualTo(Boolean.TRUE);
    Mockito.verifyNoInteractions(chain);
  }

  @Test
  @DisplayName("maps each shareable entity URL to its preview handler")
  void mapsEveryEntityType() throws Exception {
    assertForward("/backlog/1", "/preview/task/1");
    assertForward("/pitches/2", "/preview/pitch/2");
    assertForward("/cycles/3", "/preview/cycle/3");
    assertForward("/qa/bug-reports/4", "/preview/bug/4");
    assertForward("/qa/bug-reports/BUG-150", "/preview/bug/BUG-150");
    assertForward("/tasks/7", "/preview/task/7");
    assertForward("/bugs/8", "/preview/bug/8");
    assertForward("/initiatives/5", "/preview/initiative/5");
    assertForward("/wiki/1", "/preview/wiki/1");
    assertForward("/wiki/1/6", "/preview/wiki/1/6");
    assertForward("/roadmap", "/preview/roadmap");
    assertForward("/epics/9", "/preview/epic/9");
    assertForward("/backlog?project=5", "/preview/backlog?project=5");
  }

  @Test
  @DisplayName("a trailing slash and a pitch/cycle sub-page still get the item's card")
  void trailingSlashesAndSubPages() throws Exception {
    assertForward("/backlog/42/", "/preview/task/42");
    assertForward("/backlog/MARK-100", "/preview/task/100");
    assertForward("/backlog/MARK-100/", "/preview/task/100");
    assertForward("/tasks/ab2-7", "/preview/task/7");
    assertForward("/pitches/55/", "/preview/pitch/55");
    assertForward("/pitches/55/hill-chart", "/preview/pitch/55");
    assertForward("/pitches/55/test", "/preview/pitch/55");
    assertForward("/cycles/3/hill-chart", "/preview/cycle/3");
    assertForward("/cycles/3/cooldown", "/preview/cycle/3");
    assertForward("/cycles/3/qa-dashboard", "/preview/cycle/3");
    assertForward("/qa/bug-reports/BUG-150/", "/preview/bug/BUG-150");
  }

  @Test
  @DisplayName("a backlog list without a numeric project id is not previewed")
  void backlogListNeedsNumericProject() throws Exception {
    for (String uri : new String[] {"/backlog", "/backlog?project=abc", "/backlog?project="}) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(crawlerRequest(uri, "WhatsApp/2.23"), response, Mockito.mock(FilterChain.class));
      assertThat(response.getForwardedUrl()).as(uri).isNull();
    }
  }

  private void assertForward(String requestUri, String expectedForward) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(crawlerRequest(requestUri, "facebookexternalhit/1.1"), response,
        Mockito.mock(FilterChain.class));
    assertThat(response.getForwardedUrl()).isEqualTo(expectedForward);
  }

  @Test
  @DisplayName("Microsoft Teams link-preview fetchers are treated as crawlers")
  void teamsPreviewFetchersAreCrawlers() throws Exception {
    for (String ua : new String[] {
        "Mozilla/5.0 (Windows NT 6.1; WOW64) SkypeUriPreview Preview/0.5 skype-url-preview@microsoft.com",
        "Mozilla/5.0 (compatible; MicrosoftPreview/2.0; +https://aka.ms/MicrosoftPreview)"}) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(crawlerRequest("/qa/bug-reports/BUG-820", ua), response, Mockito.mock(FilterChain.class));
      assertThat(response.getForwardedUrl()).as(ua).isEqualTo("/preview/bug/BUG-820");
    }
  }

  @Test
  @DisplayName("real browsers fall through to the SPA untouched")
  void browsersPassThrough() throws Exception {
    MockHttpServletRequest request = crawlerRequest("/backlog/42",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Chrome/126 Safari/537.36");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertThat(response.getForwardedUrl()).isNull();
    Mockito.verify(chain).doFilter(request, response);
  }

  @Test
  @DisplayName("a missing User-Agent falls through rather than previewing")
  void missingUserAgentPassesThrough() throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = Mockito.mock(FilterChain.class);

    filter.doFilter(crawlerRequest("/backlog/42", null), response, chain);

    assertThat(response.getForwardedUrl()).isNull();
    Mockito.verify(chain).doFilter(Mockito.any(), Mockito.any());
  }

  @Test
  @DisplayName("crawler UAs on non-entity URLs fall through to the SPA")
  void nonEntityUrlsPassThrough() throws Exception {
    for (String uri : new String[] {"/dashboard", "/backlog", "/backlog/42/subtasks", "/blog/hello",
        "/pitches/abc", "/qa/bug-reports", "/qa/bug-reports/4/edit", "/backlog?other=5"}) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain chain = Mockito.mock(FilterChain.class);

      filter.doFilter(crawlerRequest(uri, "Slackbot-LinkExpanding 1.0"), response, chain);

      assertThat(response.getForwardedUrl()).as("forward for %s", uri).isNull();
      Mockito.verify(chain).doFilter(Mockito.any(), Mockito.any());
    }
  }

  @Test
  @DisplayName("crawler matching is case-insensitive across the known bots")
  void detectsKnownCrawlers() throws Exception {
    for (String ua : new String[] {"WhatsApp/2.23", "facebookexternalhit/1.1", "Twitterbot/1.0",
        "Slackbot-LinkExpanding 1.0", "TelegramBot (like TwitterBot)", "LinkedInBot/1.0",
        "Discordbot/2.0", "Applebot/0.1", "Googlebot/2.1", "bingbot/2.0", "embedly/0.2"}) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(crawlerRequest("/backlog/42", ua), response, Mockito.mock(FilterChain.class));
      assertThat(response.getForwardedUrl()).as("forward for UA %s", ua)
          .isEqualTo("/preview/task/42");
    }
  }
}
