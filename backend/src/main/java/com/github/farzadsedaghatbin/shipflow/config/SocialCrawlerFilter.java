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
 * Intercepts known social-crawler User-Agents visiting SPA entity URLs and redirects them to the
 * {@code /preview/**} endpoints that serve Open Graph meta tags.
 *
 * <p>Regular browser requests pass through unchanged so the React SPA loads normally.
 *
 * <p>Affected URL patterns → preview redirect:
 * <ul>
 *   <li>{@code /backlog/{id}} → {@code /preview/task/{id}}
 *   <li>{@code /pitches/{id}} → {@code /preview/pitch/{id}}
 *   <li>{@code /cycles/{id}}  → {@code /preview/cycle/{id}}
 * </ul>
 */
@Component
public class SocialCrawlerFilter extends OncePerRequestFilter {

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
        response.sendRedirect(previewUrl);
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
