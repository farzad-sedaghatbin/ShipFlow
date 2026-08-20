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
    MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
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
  }

  private void assertForward(String requestUri, String expectedForward) throws Exception {
    MockHttpServletResponse response = new MockHttpServletResponse();
    filter.doFilter(crawlerRequest(requestUri, "facebookexternalhit/1.1"), response,
        Mockito.mock(FilterChain.class));
    assertThat(response.getForwardedUrl()).isEqualTo(expectedForward);
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
        "/pitches/abc"}) {
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
