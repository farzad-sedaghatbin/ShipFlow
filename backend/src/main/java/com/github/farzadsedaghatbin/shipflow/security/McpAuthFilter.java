package com.github.farzadsedaghatbin.shipflow.security;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import com.github.farzadsedaghatbin.shipflow.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates MCP clients connecting to {@code /mcp/**} endpoints.
 *
 * <p>Two auth transports are supported:
 *
 * <ul>
 *   <li><b>Header (primary)</b> — MCP clients send their API key as a Bearer token in the
 *       Authorization header: {@code Authorization: Bearer sf_live_xxxxxxxxxxxxxxxxxxxx}. This
 *       grants the API key's real scopes ({@code SCOPE_READ} / {@code SCOPE_WRITE} / {@code
 *       SCOPE_ADMIN}) as-is.
 *   <li><b>URL path token (secondary)</b> — for clients that cannot set custom HTTP headers (e.g.
 *       free-tier claude.ai hosted connectors), the raw API key can instead be embedded as a path
 *       segment: {@code /mcp/<api-key>/sse} and {@code /mcp/<api-key>/messages} (legacy transport),
 *       or {@code /mcp/<api-key>} (Streamable HTTP transport). Because a token in a URL is far more
 *       likely to be logged by proxies, load balancers, or browser history than a header, a
 *       connection authenticated this way is <b>always capped to {@code SCOPE_READ}</b>, regardless
 *       of the underlying key's actual scopes — even a WRITE or ADMIN key only grants read access
 *       over this transport. Clients that need write access must use the header transport.
 * </ul>
 *
 * <p>Three MCP transports share this filter: the legacy HTTP+SSE transport ({@code GET /mcp/sse} +
 * {@code POST /mcp/messages}), the URL-embedded-token variant of it ({@code /mcp/<api-key>/sse|
 * messages}), and the newer Streamable HTTP transport (MCP spec 2025-06-18: a single {@code
 * POST|GET|DELETE /mcp} endpoint, or {@code /mcp/<api-key>} for the path-token variant).
 *
 * <p>This filter is always registered (regardless of {@code mcp.server.enabled}) so that Spring
 * Security can see it. It skips all non-MCP paths, so it has zero cost when MCP is disabled.
 *
 * <p>The health endpoint {@code /mcp/health} is excluded — it requires no authentication.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class McpAuthFilter extends OncePerRequestFilter {

  private static final String MCP_PATH_PREFIX = "/mcp/";

  /** The bare Streamable HTTP endpoint — no trailing slash, so it doesn't match {@link
   * #MCP_PATH_PREFIX}. Must be matched explicitly or {@link #shouldNotFilter} silently skips auth
   * for it. */
  private static final String MCP_PATH_EXACT = "/mcp";

  private static final String MCP_HEALTH_PATH = "/mcp/health";

  /** Matches the legacy URL path-token transport shape: {@code /mcp/<api-key>/sse|messages}. */
  private static final Pattern PATH_TOKEN_PATTERN = Pattern.compile("^/mcp/([^/]+)/(sse|messages)$");

  /**
   * Matches the Streamable HTTP path-token transport shape: {@code /mcp/<api-key>}. Combined with
   * {@link #RESERVED_SINGLE_SEGMENTS} below so this never mistakes the literal {@code /mcp/sse} or
   * {@code /mcp/messages} paths (legacy header-auth transport) for an API key.
   */
  private static final Pattern SINGLE_SEGMENT_PATH_TOKEN_PATTERN = Pattern.compile("^/mcp/([^/]+)$");

  /**
   * Single path segments under {@code /mcp/} that are reserved endpoint names, never API keys.
   * Excluding these from {@link #SINGLE_SEGMENT_PATH_TOKEN_PATTERN} matches prevents the new
   * 1-segment Streamable HTTP path-token shape from hijacking the existing literal {@code
   * /mcp/sse} / {@code /mcp/messages} paths (which authenticate via the {@code Authorization}
   * header, not a path segment). {@code health} is included defensively even though {@code
   * /mcp/health} is already excluded earlier in {@link #shouldNotFilter}.
   */
  private static final Set<String> RESERVED_SINGLE_SEGMENTS = Set.of("sse", "messages", "health");

  private final ApiKeyService apiKeyService;
  private final CustomUserDetailsService userDetailsService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    // Skip OPTIONS (CORS preflight)
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    // Skip anything that is not an MCP path. The bare Streamable HTTP endpoint (`/mcp`, no
    // trailing segment) must also be matched here — it doesn't start with "/mcp/" — otherwise this
    // filter is skipped entirely and the request reaches the controller with no SecurityContext.
    if (!uri.equals(MCP_PATH_EXACT) && !uri.startsWith(MCP_PATH_PREFIX)) {
      return true;
    }
    // Health check is public — no auth needed
    if (uri.equals(MCP_HEALTH_PATH)) {
      return true;
    }
    // Skip if already authenticated (e.g., re-used connection)
    return SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().isAuthenticated();
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String requestUri = request.getRequestURI();
    Matcher pathTokenMatcher = PATH_TOKEN_PATTERN.matcher(requestUri);
    boolean viaPathToken = pathTokenMatcher.matches();
    String pathTokenGroup1 = viaPathToken ? pathTokenMatcher.group(1) : null;

    if (!viaPathToken) {
      // Only treat the 1-segment shape as a path-token match when the segment isn't a reserved
      // endpoint name (sse/messages/health) — otherwise this would hijack the legacy header-auth
      // transport's literal /mcp/sse and /mcp/messages paths.
      Matcher singleSegmentMatcher = SINGLE_SEGMENT_PATH_TOKEN_PATTERN.matcher(requestUri);
      if (singleSegmentMatcher.matches()
          && !RESERVED_SINGLE_SEGMENTS.contains(singleSegmentMatcher.group(1))) {
        viaPathToken = true;
        pathTokenGroup1 = singleSegmentMatcher.group(1);
      }
    }

    String rawKey;
    if (viaPathToken) {
      rawKey = pathTokenGroup1.trim();
      if (rawKey.isEmpty()) {
        sendUnauthorized(response, "Empty API key in URL path");
        return;
      }
    } else {
      String authHeader = request.getHeader("Authorization");
      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        sendUnauthorized(response, "Missing or malformed Authorization header. Expected: Bearer <api-key>");
        return;
      }

      rawKey = authHeader.substring(7).trim();
      if (rawKey.isEmpty()) {
        sendUnauthorized(response, "Empty API key in Authorization header");
        return;
      }
    }

    Optional<ApiKey> apiKeyOpt = apiKeyService.validateKey(rawKey);
    if (apiKeyOpt.isEmpty()) {
      sendUnauthorized(response, "Invalid or expired API key");
      return;
    }

    ApiKey apiKey = apiKeyOpt.get();
    apiKeyService.recordUsage(apiKey);

    // Enforce write scope: POST to /mcp/messages with write tools requires WRITE or ADMIN
    Set<ApiKeyScope> scopes = apiKey.getScopes();
    String method = request.getMethod().toUpperCase();
    if ("POST".equals(method) && !scopes.contains(ApiKeyScope.WRITE) && !scopes.contains(ApiKeyScope.ADMIN)) {
      // Write enforcement happens in the tool dispatcher based on the tool name;
      // here we allow the request through and let the dispatcher reject write tools.
    }

    // Scope authorities from the API key (READ / WRITE / ADMIN) — capped to SCOPE_READ when
    // authenticated via the weaker URL path-token transport, regardless of the key's real scopes.
    List<GrantedAuthority> authorities;
    if (viaPathToken) {
      authorities = new ArrayList<>();
      authorities.add(new SimpleGrantedAuthority("SCOPE_READ"));
      log.info("MCP client authenticated via URL path token (read-only), key={}", apiKey.getKeyPrefix());
    } else {
      authorities = new ArrayList<>(
          scopes.stream()
              .map(s -> new SimpleGrantedAuthority("SCOPE_" + s.name()))
              .collect(Collectors.toList()));
    }

    // Merge with the user's role authorities so @PreAuthorize / hasRole checks still work
    UserDetails userDetails = userDetailsService.loadUserByUsername(apiKey.getUser().getUsername());
    authorities.addAll(userDetails.getAuthorities());

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    // Store the ApiKey entity in details so downstream services (e.g. McpUsageReportService)
    // can record which key was used without an extra DB look-up.
    auth.setDetails(apiKey);
    SecurityContextHolder.getContext().setAuthentication(auth);

    filterChain.doFilter(request, response);
  }

  private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
