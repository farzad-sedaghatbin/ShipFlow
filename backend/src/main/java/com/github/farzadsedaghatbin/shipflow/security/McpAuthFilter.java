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
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates MCP clients connecting to {@code /mcp/**} endpoints.
 *
 * <p>MCP clients send their API key as a Bearer token in the Authorization header:
 *
 * <pre>Authorization: Bearer sf_live_xxxxxxxxxxxxxxxxxxxx</pre>
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
  private static final String MCP_HEALTH_PATH = "/mcp/health";

  private final ApiKeyService apiKeyService;
  private final CustomUserDetailsService userDetailsService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    String uri = request.getRequestURI();
    // Skip OPTIONS (CORS preflight)
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    // Skip anything that is not an MCP path
    if (!uri.startsWith(MCP_PATH_PREFIX)) {
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

    String authHeader = request.getHeader("Authorization");
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      sendUnauthorized(response, "Missing or malformed Authorization header. Expected: Bearer <api-key>");
      return;
    }

    String rawKey = authHeader.substring(7).trim();
    if (rawKey.isEmpty()) {
      sendUnauthorized(response, "Empty API key in Authorization header");
      return;
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

    // Scope authorities from the API key (READ / WRITE / ADMIN)
    List<GrantedAuthority> authorities = new ArrayList<>(
        scopes.stream()
            .map(s -> new SimpleGrantedAuthority("SCOPE_" + s.name()))
            .collect(Collectors.toList()));

    // Merge with the user's role authorities so @PreAuthorize / hasRole checks still work
    UserDetails userDetails = userDetailsService.loadUserByUsername(apiKey.getUser().getUsername());
    authorities.addAll(userDetails.getAuthorities());

    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(userDetails, null, authorities);
    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(auth);

    filterChain.doFilter(request, response);
  }

  private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
    response.setContentType("application/json");
    response.getWriter().write("{\"error\":\"" + message + "\"}");
  }
}
