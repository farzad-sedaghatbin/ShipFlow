package com.github.farzadsedaghatbin.shipflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Intercepts known social-crawler User-Agents visiting SPA entity URLs and serves the Open Graph
 * page from {@code /preview/**} <em>in place</em>, at the URL that was actually shared.
 *
 * <p>Regular browser requests pass through unchanged so the React SPA loads normally.
 *
 * <p>Affected URL patterns → preview handler:
 * <ul>
 *   <li>{@code /backlog/{id}} and legacy {@code /tasks/{id}} → {@code /preview/task/{id}}
 *   <li>{@code /qa/bug-reports/{id or key}} and legacy {@code /bugs/{id}} → {@code /preview/bug/{id or key}}
 *   <li>{@code /pitches/{id}} (and its hill-chart/test sub-pages) → {@code /preview/pitch/{id}}
 *   <li>{@code /cycles/{id}} (and its hill-chart/cooldown/… sub-pages) → {@code /preview/cycle/{id}}
 *   <li>{@code /epics/{id}} → {@code /preview/epic/{id}}
 *   <li>{@code /backlog?project={id}} → {@code /preview/backlog?project={id}}
 *   <li>{@code /initiatives/{id}} → {@code /preview/initiative/{id}}
 *   <li>{@code /wiki/{spaceId}} and {@code /wiki/{spaceId}/{pageId}} → {@code /preview/wiki/...}
 *   <li>{@code /roadmap} → {@code /preview/roadmap}
 * </ul>
 *
 * <p><strong>Why a forward and not a redirect.</strong> This used to answer crawlers with
 * {@code sendRedirect("/preview/task/1")}. The servlet container turns a relative redirect into an
 * absolute {@code Location} built from the request's own scheme — and behind a TLS-terminating
 * proxy that scheme is {@code http}, so a crawler that fetched {@code https://host/backlog/1} was
 * handed {@code http://host/preview/task/1}: an HTTPS→HTTP downgrade, followed by the edge bouncing
 * it back up to HTTPS. Several crawlers (notably Twitterbot and WhatsApp) refuse to follow a
 * protocol downgrade, so the shared link previewed as nothing at all. Forwarding keeps the response
 * at the shared URL — one hop, one scheme, canonical {@code og:url} — and needs nothing of the
 * crawler beyond reading the body it already asked for.
 *
 * <p><strong>Filter order.</strong> This must run strictly after {@code ForwardedHeaderFilter}
 * (so the request already carries the corrected scheme/host before the forward — see the
 * class-level "why a forward" note above) and after Spring Security's chain (so the SPA-route
 * authorization decision for the original URL is what applies, not one for {@code /preview/**}).
 * Both currently precede an unordered filter by Spring Boot's defaults, but that's an assumption
 * this class previously left unpinned — {@code @Order} makes it explicit instead of relying on
 * default precedence surviving future filter additions or a Spring Boot upgrade.
 */
@Component
@Order(SocialCrawlerFilter.FILTER_ORDER)
public class SocialCrawlerFilter extends OncePerRequestFilter {

  /**
   * Comfortably after Spring Security's chain ({@code SecurityProperties.DEFAULT_FILTER_ORDER},
   * {@code -100}) and after {@code ForwardedHeaderFilter} (registered near
   * {@code Ordered.HIGHEST_PRECEDENCE} when {@code server.forward-headers-strategy=framework}),
   * but still ahead of {@code Ordered.LOWEST_PRECEDENCE} where an unordered filter would land.
   */
  static final int FILTER_ORDER = Ordered.LOWEST_PRECEDENCE - 1000;

  /**
   * Set on the request before forwarding, so {@link
   * com.github.farzadsedaghatbin.shipflow.controller.LinkPreviewController} knows its HTML is being
   * served at the canonical entity URL rather than under {@code /preview/**}, and omits the
   * bounce-to-the-SPA script that would otherwise reload this very URL forever.
   */
  public static final String IN_PLACE_ATTRIBUTE = "shipflow.linkPreview.inPlace";

  private static final Set<String> CRAWLER_UA_FRAGMENTS = Set.of(
      "whatsapp",
      "facebookexternalhit",
      "facebookcatalog",
      "twitterbot",
      "slackbot",
      "slack-imgproxy",
      "telegrambot",
      "linkedinbot",
      "discordbot",
      "applebot",
      // Microsoft Teams (and Skype/Outlook) fetch link previews with these, not with a browser UA
      "skypeuripreview",
      "skype-url-preview",
      "microsoftpreview",
      "googlebot",
      "bingbot",
      "ia_archiver",
      "embedly",
      "outbrain",
      "pinterest"
  );

  private static final Pattern TASK_PATH   = Pattern.compile("^/(?:backlog|tasks)/(\\d+)$");
  // The bug page settles on the bug key (/qa/bug-reports/BUG-150) — that is the URL people copy —
  // while older links and notifications still carry the numeric id.
  private static final Pattern BUG_PATH    = Pattern.compile("^/(?:qa/bug-reports|bugs)/([A-Za-z0-9-]{1,64})$");
  // Sub-pages of a pitch/cycle (hill chart, QA, cooldown…) are shared as often as the pitch itself,
  // and show the same item, so they get the item's card rather than the generic site one.
  private static final Pattern PITCH_PATH  =
      Pattern.compile("^/pitches/(\\d+)(?:/(?:hill-chart|test))?$");
  private static final Pattern CYCLE_PATH  =
      Pattern.compile("^/cycles/(\\d+)(?:/(?:hill-chart|circuit-breaker|cooldown|qa-dashboard))?$");
  private static final Pattern EPIC_PATH   = Pattern.compile("^/epics/(\\d+)$");
  private static final Pattern PROJECT_PARAM = Pattern.compile("^\\d+$");
  private static final Pattern INITIATIVE_PATH = Pattern.compile("^/initiatives/(\\d+)$");
  private static final Pattern WIKI_SPACE_PATH = Pattern.compile("^/wiki/(\\d+)$");
  private static final Pattern WIKI_PAGE_PATH  = Pattern.compile("^/wiki/(\\d+)/(\\d+)$");

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    String ua = request.getHeader("User-Agent");
    if (ua != null && isCrawler(ua)) {
      String previewUrl = resolvePreviewUrl(normalise(request.getRequestURI()), request.getParameter("project"));
      if (previewUrl != null) {
        request.setAttribute(IN_PLACE_ATTRIBUTE, Boolean.TRUE);
        request.getRequestDispatcher(previewUrl).forward(request, response);
        return;
      }
    }

    chain.doFilter(request, response);
  }

  /** {@code /backlog/42/} is the same page as {@code /backlog/42} — the SPA router treats it so. */
  private static String normalise(String path) {
    return path.length() > 1 && path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
  }

  private boolean isCrawler(String ua) {
    String lower = ua.toLowerCase();
    for (String fragment : CRAWLER_UA_FRAGMENTS) {
      if (lower.contains(fragment)) {
        return true;
      }
    }
    return false;
  }

  private String resolvePreviewUrl(String path, String projectParam) {
    Matcher m;

    m = TASK_PATH.matcher(path);
    if (m.matches()) return "/preview/task/" + m.group(1);

    m = BUG_PATH.matcher(path);
    if (m.matches()) return "/preview/bug/" + m.group(1);

    m = PITCH_PATH.matcher(path);
    if (m.matches()) return "/preview/pitch/" + m.group(1);

    m = CYCLE_PATH.matcher(path);
    if (m.matches()) return "/preview/cycle/" + m.group(1);

    m = EPIC_PATH.matcher(path);
    if (m.matches()) return "/preview/epic/" + m.group(1);

    m = INITIATIVE_PATH.matcher(path);
    if (m.matches()) return "/preview/initiative/" + m.group(1);

    m = WIKI_SPACE_PATH.matcher(path);
    if (m.matches()) return "/preview/wiki/" + m.group(1);

    m = WIKI_PAGE_PATH.matcher(path);
    if (m.matches()) return "/preview/wiki/" + m.group(1) + "/" + m.group(2);

    if ("/roadmap".equals(path)) return "/preview/roadmap";

    // The backlog list lives at /backlog?project=N — only a numeric project id is forwarded.
    if ("/backlog".equals(path) && projectParam != null && PROJECT_PARAM.matcher(projectParam).matches()) {
      return "/preview/backlog?project=" + projectParam;
    }

    return null;
  }
}
