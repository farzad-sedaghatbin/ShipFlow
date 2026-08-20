package com.github.farzadsedaghatbin.shipflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
 *   <li>{@code /backlog/{id}} → {@code /preview/task/{id}}
 *   <li>{@code /pitches/{id}} → {@code /preview/pitch/{id}}
 *   <li>{@code /cycles/{id}}  → {@code /preview/cycle/{id}}
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
 */
@Component
public class SocialCrawlerFilter extends OncePerRequestFilter {

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
      "googlebot",
      "bingbot",
      "ia_archiver",
      "embedly",
      "outbrain",
      "pinterest"
  );

  private static final Pattern TASK_PATH   = Pattern.compile("^/backlog/(\\d+)$");
  private static final Pattern PITCH_PATH  = Pattern.compile("^/pitches/(\\d+)$");
  private static final Pattern CYCLE_PATH  = Pattern.compile("^/cycles/(\\d+)$");

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    String ua = request.getHeader("User-Agent");
    if (ua != null && isCrawler(ua)) {
      String previewUrl = resolvePreviewUrl(request.getRequestURI());
      if (previewUrl != null) {
        request.setAttribute(IN_PLACE_ATTRIBUTE, Boolean.TRUE);
        request.getRequestDispatcher(previewUrl).forward(request, response);
        return;
      }
    }

    chain.doFilter(request, response);
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

  private String resolvePreviewUrl(String path) {
    Matcher m;

    m = TASK_PATH.matcher(path);
    if (m.matches()) return "/preview/task/" + m.group(1);

    m = PITCH_PATH.matcher(path);
    if (m.matches()) return "/preview/pitch/" + m.group(1);

    m = CYCLE_PATH.matcher(path);
    if (m.matches()) return "/preview/cycle/" + m.group(1);

    return null;
  }
}
