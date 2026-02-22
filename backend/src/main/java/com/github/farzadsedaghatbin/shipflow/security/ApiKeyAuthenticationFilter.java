package com.github.farzadsedaghatbin.shipflow.security;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import com.github.farzadsedaghatbin.shipflow.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Authenticates requests to /api/v1/public/** using the X-API-Key header.
 * If a valid API key is found, the associated user is placed into the SecurityContext.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ApiKeyAuthenticationFilter extends OncePerRequestFilter {

  private static final String API_KEY_HEADER = "X-API-Key";
  private static final String PUBLIC_API_PREFIX = "/api/v1/public/";

  private final ApiKeyService apiKeyService;
  private final CustomUserDetailsService userDetailsService;

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) {
    // Only apply to paths that start with the public API prefix.
    // Always skip OPTIONS so CORS preflight requests are not rejected with 401.
    if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
      return true;
    }
    return !request.getRequestURI().startsWith(PUBLIC_API_PREFIX);
  }

  @Override
  protected void doFilterInternal(HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain) throws ServletException, IOException {

    // Skip if already authenticated (e.g. JWT from an internal call)
    if (SecurityContextHolder.getContext().getAuthentication() != null
        && SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
      filterChain.doFilter(request, response);
      return;
    }

    String rawKey = request.getHeader(API_KEY_HEADER);
    if (rawKey == null || rawKey.isBlank()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Missing X-API-Key header\"}");
      return;
    }

    Optional<ApiKey> apiKeyOpt = apiKeyService.validateKey(rawKey);
    if (apiKeyOpt.isEmpty()) {
      response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"Invalid or expired API key\"}");
      return;
    }

    ApiKey apiKey = apiKeyOpt.get();
    apiKeyService.recordUsage(apiKey);

    // Enforce scope: mutating methods require WRITE or ADMIN scope
    Set<ApiKeyScope> scopes = apiKey.getScopes();
    String method = request.getMethod().toUpperCase();
    boolean isMutating = List.of("POST", "PUT", "PATCH", "DELETE").contains(method);
    if (isMutating && !scopes.contains(ApiKeyScope.WRITE) && !scopes.contains(ApiKeyScope.ADMIN)) {
      response.setStatus(HttpServletResponse.SC_FORBIDDEN);
      response.setContentType("application/json");
      response.getWriter().write("{\"error\":\"API key scope is insufficient for this operation. WRITE or ADMIN scope required.\"}");
      return;
    }

    // Build authorities from the key's own scopes, not the user's full authority set,
    // so a READ-scoped key cannot act with full user privileges.
    List<GrantedAuthority> authorities = scopes.stream()
        .map(s -> new SimpleGrantedAuthority("SCOPE_" + s.name()))
        .collect(Collectors.toList());

    UserDetails userDetails = userDetailsService
        .loadUserByUsername(apiKey.getUser().getUsername());

    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
        userDetails, null, authorities);
    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
    SecurityContextHolder.getContext().setAuthentication(auth);

    filterChain.doFilter(request, response);
  }
}
