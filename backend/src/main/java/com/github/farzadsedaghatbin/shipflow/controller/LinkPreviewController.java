package com.github.farzadsedaghatbin.shipflow.controller;

import com.github.farzadsedaghatbin.shipflow.config.SocialCrawlerFilter;
import com.github.farzadsedaghatbin.shipflow.entity.Cycle;
import com.github.farzadsedaghatbin.shipflow.entity.Pitch;
import com.github.farzadsedaghatbin.shipflow.entity.Task;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.TaskRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.HtmlUtils;

/**
 * Publicly accessible controller that returns HTML pages containing Open Graph meta tags for
 * task, pitch, and cycle URLs. Social platforms (Slack, iMessage, etc.) fetch these pages to
 * generate rich link previews. After the OG crawl the page immediately redirects the user's
 * browser back to the SPA.
 *
 * <p>Endpoints are at {@code /preview/**}, NOT under {@code /api/**}, so they are reachable
 * without a JWT token. Only titles and truncated descriptions are exposed — no sensitive data.
 */
@RestController
@RequiredArgsConstructor
public class LinkPreviewController {

  private static final String OG_IMAGE_PATH = "/android-chrome-512x512.png";
  private static final int MAX_DESC_LENGTH = 160;

  private final PitchRepository pitchRepository;
  private final TaskRepository taskRepository;
  private final CycleRepository cycleRepository;

  /**
   * Same override the app already uses for OAuth callback and webhook URLs
   * ({@code GitHubAppOAuthService}, {@code InboundWebhookConfigService}) — set when a
   * self-hoster's request-derived origin isn't the public one (a proxy that doesn't forward
   * {@code X-Forwarded-*}, non-standard routing). Falls back to request derivation when unset.
   */
  @Value("${app.base-url:}")
  private String configuredBaseUrl;

  // -------------------------------------------------------------------------
  // Pitch preview
  // -------------------------------------------------------------------------

  @GetMapping(value = "/preview/pitch/{id}", produces = "text/html;charset=UTF-8")
  public ResponseEntity<String> pitchPreview(@PathVariable Long id) {
    Optional<Pitch> opt = pitchRepository.findById(id);
    if (opt.isEmpty()) {
      return notFound("Pitch not found", "/pitches/" + id);
    }
    Pitch pitch = opt.get();
    String title = pitch.getTitle();
    String raw = pitch.getProblemStatement() != null ? pitch.getProblemStatement()
        : (pitch.getDescription() != null ? pitch.getDescription() : "");
    String description = truncate(stripMarkdown(raw));
    return preview(HttpStatus.OK, buildPreviewHtml(title, description, "/pitches/" + id));
  }

  // -------------------------------------------------------------------------
  // Task preview
  // -------------------------------------------------------------------------

  @GetMapping(value = "/preview/task/{id}", produces = "text/html;charset=UTF-8")
  public ResponseEntity<String> taskPreview(@PathVariable Long id) {
    Optional<Task> opt = taskRepository.findById(id);
    if (opt.isEmpty()) {
      return notFound("Task not found", "/backlog/" + id);
    }
    Task task = opt.get();
    String title = task.getTitle();
    String raw = task.getDescription() != null ? task.getDescription() : "";
    String description = truncate(stripMarkdown(raw));
    return preview(HttpStatus.OK, buildPreviewHtml(title, description, "/backlog/" + id));
  }

  // -------------------------------------------------------------------------
  // Cycle preview
  // -------------------------------------------------------------------------

  @GetMapping(value = "/preview/cycle/{id}", produces = "text/html;charset=UTF-8")
  public ResponseEntity<String> cyclePreview(@PathVariable Long id) {
    Optional<Cycle> opt = cycleRepository.findById(id);
    if (opt.isEmpty()) {
      return notFound("Cycle not found", "/cycles/" + id);
    }
    Cycle cycle = opt.get();
    String title = cycle.getName();
    // Counted with a query, NOT via cycle.getPitches().size(): that collection is lazy and this
    // controller runs outside any transaction, so touching it threw LazyInitializationException and
    // every cycle preview 500'd — no preview has ever rendered for a cycle link. The count query
    // also excludes soft-deleted pitches, which the collection did not.
    long pitchCount = pitchRepository.countByCycleIdNotDeleted(id);
    String description = String.format(
        "Cycle: %d pitch%s · %s – %s",
        pitchCount,
        pitchCount == 1 ? "" : "es",
        cycle.getStartDate(),
        cycle.getEndDate());
    return preview(HttpStatus.OK, buildPreviewHtml(title, description, "/cycles/" + id));
  }

  // -------------------------------------------------------------------------
  // Helpers
  // -------------------------------------------------------------------------

  /**
   * {@code canonicalPath} is the entity's own URL (e.g. {@code /backlog/999}), not a generic
   * list fallback — an id that 404s here still deserves an accurate {@code og:url}, and the
   * SPA's own route can render its usual "not found" state for it.
   */
  private ResponseEntity<String> notFound(String reason, String canonicalPath) {
    String html = buildPreviewHtml(
        "ShipFlow",
        "Project management built around Shape Up.",
        canonicalPath);
    return preview(HttpStatus.NOT_FOUND, html);
  }

  /**
   * Wraps the HTML with {@code Cache-Control: no-store}. Serving this from a forward means the
   * response can be cached under the *shared* URL a real browser also uses (the forward keeps
   * the address bar on e.g. {@code /backlog/1}, not {@code /preview/task/1}) — without this, a
   * CDN that caches the first response it sees for that URL could serve the crawler-only OG stub
   * to real visitors, or vice versa.
   */
  private ResponseEntity<String> preview(HttpStatus status, String html) {
    return ResponseEntity.status(status).cacheControl(CacheControl.noStore()).body(html);
  }

  /**
   * Absolute origin of the instance actually being browsed, e.g. {@code https://shipflow.dev} or
   * {@code https://shipflow.acme.internal}.
   *
   * <p>These URLs used to be hardcoded to {@code https://shipflow.dev}, which meant every
   * self-hosted deployment advertised someone else's host in {@code og:url} and {@code og:image} —
   * previews of a private instance linked to the public demo, and the image 404'd or leaked the
   * demo's branding. {@code app.base-url}, when configured, wins outright — it needs no proxy
   * cooperation and is the same knob self-hosters already reach for elsewhere. Otherwise this
   * derives the origin from the current request, which behind a TLS-terminating proxy needs
   * {@code X-Forwarded-Proto} to be honoured — {@code server.forward-headers-strategy=framework}
   * (set in the prod profile) takes care of that.
   */
  private String currentOrigin() {
    if (configuredBaseUrl != null && !configuredBaseUrl.isBlank()) {
      return configuredBaseUrl;
    }
    return ServletUriComponentsBuilder.fromCurrentContextPath().replacePath(null).build()
        .toUriString();
  }

  /**
   * True when {@link SocialCrawlerFilter} forwarded a crawler here, so this HTML is already being
   * served at the entity's canonical URL.
   */
  private boolean servedInPlace() {
    return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs
        && Boolean.TRUE.equals(attrs.getRequest().getAttribute(SocialCrawlerFilter.IN_PLACE_ATTRIBUTE));
  }

  private String buildPreviewHtml(String title, String description, String canonicalPath) {
    String safeTitle = HtmlUtils.htmlEscape(title);
    String safeDesc = HtmlUtils.htmlEscape(description);
    String safeRedirect = HtmlUtils.htmlEscape(canonicalPath);
    String ogUrl = currentOrigin() + canonicalPath;
    String ogImage = currentOrigin() + OG_IMAGE_PATH;
    // Served in place, the bounce script would point at the URL we are already on and reload
    // forever — which JS-executing crawlers (Googlebot renders pages) would actually do. Only the
    // direct /preview/** hit, a genuinely different URL, gets the bounce.
    String bounce = servedInPlace()
        ? ""
        : "<script>window.location.replace('%s');</script>".formatted(safeRedirect);
    return """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8"/>
          <meta property="og:type" content="website"/>
          <meta property="og:title" content="%s — ShipFlow"/>
          <meta property="og:description" content="%s"/>
          <meta property="og:url" content="%s"/>
          <meta property="og:image" content="%s"/>
          <meta property="og:site_name" content="ShipFlow"/>
          <meta name="twitter:card" content="summary"/>
          <meta name="twitter:title" content="%s — ShipFlow"/>
          <meta name="twitter:description" content="%s"/>
          <title>%s — ShipFlow</title>
          %s
        </head>
        <body><p><a href="%s">%s</a></p></body>
        </html>
        """.formatted(
        safeTitle, safeDesc, ogUrl, ogImage,
        safeTitle, safeDesc,
        safeTitle, bounce,
        safeRedirect, safeTitle);
  }

  /** Remove the most common Markdown syntax so descriptions read cleanly in OG previews. */
  private String stripMarkdown(String text) {
    if (text == null || text.isEmpty()) {
      return "";
    }
    return text
        // Headings
        .replaceAll("(?m)^#{1,6}\\s+", "")
        // Bold / italic
        .replaceAll("\\*{1,3}(.+?)\\*{1,3}", "$1")
        .replaceAll("_{1,3}(.+?)_{1,3}", "$1")
        // Inline code
        .replaceAll("`{1,3}(.+?)`{1,3}", "$1")
        // Links [text](url) → text
        .replaceAll("\\[([^]]+)]\\([^)]+\\)", "$1")
        // Images
        .replaceAll("!\\[[^]]*]\\([^)]+\\)", "")
        // Blockquotes and list markers
        .replaceAll("(?m)^[>*+-]\\s+", "")
        // Horizontal rules
        .replaceAll("(?m)^[-*_]{3,}\\s*$", "")
        // Extra whitespace / newlines → single space
        .replaceAll("[\\r\\n]+", " ")
        .replaceAll("\\s{2,}", " ")
        .trim();
  }

  private String truncate(String text) {
    if (text == null) {
      return "";
    }
    return text.length() <= MAX_DESC_LENGTH ? text
        : text.substring(0, MAX_DESC_LENGTH - 1) + "…";
  }
}
