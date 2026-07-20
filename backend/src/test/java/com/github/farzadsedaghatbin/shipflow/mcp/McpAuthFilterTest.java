package com.github.farzadsedaghatbin.shipflow.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.ApiKey;
import com.github.farzadsedaghatbin.shipflow.entity.enums.ApiKeyScope;
import com.github.farzadsedaghatbin.shipflow.security.CustomUserDetailsService;
import com.github.farzadsedaghatbin.shipflow.security.McpAuthFilter;
import com.github.farzadsedaghatbin.shipflow.service.ApiKeyService;
import jakarta.servlet.FilterChain;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Unit tests for {@link McpAuthFilter} — exercises the full filter chain via {@code doFilter()} so
 * we test the real behaviour without needing to access protected methods.
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
class McpAuthFilterTest {

  @Mock private ApiKeyService apiKeyService;
  @Mock private CustomUserDetailsService userDetailsService;
  @Mock private FilterChain filterChain;

  private McpAuthFilter filter;

  @BeforeEach
  void setUp() {
    filter = new McpAuthFilter(apiKeyService, userDetailsService);
    SecurityContextHolder.clearContext();
  }

  // ── paths that should be skipped (no auth attempted) ──────────────────────

  @Test
  void nonMcpPath_skipsBearerCheck() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/projects");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    // Filter skipped — chain must be called, no 401
    verify(filterChain).doFilter(req, res);
    assertThat(res.getStatus()).isNotEqualTo(401);
  }

  @Test
  void mcpHealthPath_skipsBearerCheck() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/health");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    verify(filterChain).doFilter(req, res);
    assertThat(res.getStatus()).isNotEqualTo(401);
  }

  @Test
  void optionsRequest_skipsBearerCheck() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/mcp/sse");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    verify(filterChain).doFilter(req, res);
    assertThat(res.getStatus()).isNotEqualTo(401);
  }

  // ── missing / malformed header ─────────────────────────────────────────────

  @Test
  void missingAuthHeader_returns401() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Missing or malformed");
    verify(filterChain, never()).doFilter(req, res);
  }

  @Test
  void wrongAuthScheme_returns401() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    req.addHeader("Authorization", "Basic dXNlcjpwYXNz");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Missing or malformed");
  }

  @Test
  void emptyBearerToken_returns401() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    req.addHeader("Authorization", "Bearer ");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Empty API key");
  }

  // ── invalid key ────────────────────────────────────────────────────────────

  @Test
  void invalidApiKey_returns401() throws Exception {
    when(apiKeyService.validateKey("bad-key")).thenReturn(Optional.empty());

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    req.addHeader("Authorization", "Bearer bad-key");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Invalid or expired");
    verify(filterChain, never()).doFilter(req, res);
  }

  // ── valid READ key ─────────────────────────────────────────────────────────

  @Test
  void validReadKey_setsAuthenticationAndContinues() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("alice");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ));
    apiKey.setUser(entityUser);

    when(apiKeyService.validateKey("sf_live_alice")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("alice")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    req.addHeader("Authorization", "Bearer sf_live_alice");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isNotEqualTo(401);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(
            SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("SCOPE_READ")))
        .isTrue();
    verify(filterChain).doFilter(req, res);
    verify(apiKeyService).recordUsage(apiKey);
  }

  // ── valid WRITE key ────────────────────────────────────────────────────────

  @Test
  void validWriteKey_includesWriteScope() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("bob");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE));
    apiKey.setUser(entityUser);

    when(apiKeyService.validateKey("sf_live_bob")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("bob")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("bob")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp/messages");
    req.addHeader("Authorization", "Bearer sf_live_bob");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_READ"))).isTrue();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    verify(filterChain).doFilter(req, res);
  }

  // ── response format ────────────────────────────────────────────────────────

  @Test
  void unauthorizedResponse_isJson() throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getContentType()).contains("application/json");
  }

  // ── URL path-token transport ───────────────────────────────────────────────

  @Test
  void pathToken_validKeyWithWriteScope_respectsRealScope() throws Exception {
    // Path-token auth is no longer read-only-capped — it grants exactly what the key was created
    // with, same as header auth (see the McpAuthFilter class Javadoc for the rationale: claude.ai's
    // free-tier connector can't send a header, so the URL *is* the only auth channel it has; the
    // safety net is scoping/rotating the key, not silently downgrading it server-side).
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("alice");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE, ApiKeyScope.ADMIN));
    apiKey.setUser(entityUser);
    apiKey.setKeyPrefix("sf_live_a");

    when(apiKeyService.validateKey("sf_live_alice")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("alice")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sf_live_alice/sse");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isNotEqualTo(401);
    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_READ"))).isTrue();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"))).isTrue();
    verify(filterChain).doFilter(req, res);
    verify(apiKeyService).recordUsage(apiKey);
  }

  @Test
  void pathToken_invalidKey_returns401() throws Exception {
    when(apiKeyService.validateKey("bad-key")).thenReturn(Optional.empty());

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/bad-key/sse");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Invalid or expired");
    verify(filterChain, never()).doFilter(req, res);
  }

  @Test
  void pathToken_appliesToMessagesPath() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("alice");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE, ApiKeyScope.ADMIN));
    apiKey.setUser(entityUser);
    apiKey.setKeyPrefix("sf_live_a");

    when(apiKeyService.validateKey("sf_live_alice")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("alice")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp/sf_live_alice/messages");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isNotEqualTo(401);
    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_READ"))).isTrue();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    verify(filterChain).doFilter(req, res);
  }

  @Test
  void headerAuth_stillWorksUnchanged() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("bob");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE));
    apiKey.setUser(entityUser);

    when(apiKeyService.validateKey("sf_live_bob")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("bob")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("bob")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    req.addHeader("Authorization", "Bearer sf_live_bob");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isNotEqualTo(401);
    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    // Header auth must NOT be capped — WRITE scope must survive intact.
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_READ"))).isTrue();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    verify(filterChain).doFilter(req, res);
  }

  @Test
  void pathToken_emptyKeySegment_returns401() throws Exception {
    // /mcp//sse — MockHttpServletRequest does not collapse the double slash, so this exercises
    // whichever branch the filter actually takes for an empty path segment.
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp//sse");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    verify(filterChain, never()).doFilter(req, res);
  }

  // ── bare /mcp Streamable HTTP endpoint — shouldNotFilter bug fix ──────────────

  @Test
  void bareMcpPath_missingAuthHeader_returns401_notSilentlySkipped() throws Exception {
    // Regression test for the shouldNotFilter bug: "/mcp" (no trailing segment) does not start
    // with MCP_PATH_PREFIX ("/mcp/"), so before the fix `!uri.startsWith(MCP_PATH_PREFIX)` was
    // true and shouldNotFilter() returned true, skipping auth entirely — the request would reach
    // the controller completely unauthenticated instead of being rejected. Post-fix, this path
    // must be treated exactly like any other MCP path: no Authorization header -> 401.
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Missing or malformed");
    verify(filterChain, never()).doFilter(req, res);
  }

  @Test
  void bareMcpPath_validBearerToken_authenticatesAndContinues() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("alice");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE));
    apiKey.setUser(entityUser);

    when(apiKeyService.validateKey("sf_live_alice")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("alice")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp");
    req.addHeader("Authorization", "Bearer sf_live_alice");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isNotEqualTo(401);
    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    // Header auth on the bare Streamable HTTP endpoint must not be capped, same as the legacy
    // header-auth transport.
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    verify(filterChain).doFilter(req, res);
  }

  // ── Streamable HTTP 1-segment path-token shape: /mcp/<api-key> ────────────────

  @Test
  void streamableHttpPathToken_validKeyWithWriteScope_respectsRealScope() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("alice");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE, ApiKeyScope.ADMIN));
    apiKey.setUser(entityUser);
    apiKey.setKeyPrefix("sf_live_a");

    when(apiKeyService.validateKey("sf_live_alice")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("alice")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp/sf_live_alice");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isNotEqualTo(401);
    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_READ"))).isTrue();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"))).isTrue();
    verify(filterChain).doFilter(req, res);
    verify(apiKeyService).recordUsage(apiKey);
  }

  @Test
  void streamableHttpPathToken_getAndDelete_alsoRespectRealScope() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("alice");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE));
    apiKey.setUser(entityUser);
    apiKey.setKeyPrefix("sf_live_a");

    when(apiKeyService.validateKey("sf_live_alice")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("alice")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("alice")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("DELETE", "/mcp/sf_live_alice");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    verify(filterChain).doFilter(req, res);
  }

  // ── reserved-segment collision guard: /mcp/sse and /mcp/messages must NOT be hijacked ─────────

  @Test
  void literalSsePath_stillRequiresAndReadsAuthorizationHeader_notTreatedAsPathToken() throws Exception {
    // Regression guard: the new 1-segment pattern (^/mcp/([^/]+)$) would, without the reserved-
    // segment exclusion, match the literal "/mcp/sse" and treat "sse" as if it were the API key —
    // completely breaking the existing legacy header-based transport. Confirm the literal path
    // still requires the Authorization header exactly as before.
    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Missing or malformed");
    verify(filterChain, never()).doFilter(req, res);
    // Must never have attempted to validate "sse" as an API key.
    verify(apiKeyService, never()).validateKey("sse");
  }

  @Test
  void literalMessagesPath_stillRequiresAndReadsAuthorizationHeader_notTreatedAsPathToken()
      throws Exception {
    MockHttpServletRequest req = new MockHttpServletRequest("POST", "/mcp/messages");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isEqualTo(401);
    assertThat(res.getContentAsString()).contains("Missing or malformed");
    verify(filterChain, never()).doFilter(req, res);
    verify(apiKeyService, never()).validateKey("messages");
  }

  @Test
  void literalSsePath_withValidHeaderAuth_stillWorksUnchanged() throws Exception {
    com.github.farzadsedaghatbin.shipflow.entity.User entityUser =
        new com.github.farzadsedaghatbin.shipflow.entity.User();
    entityUser.setUsername("bob");

    ApiKey apiKey = new ApiKey();
    apiKey.setScopes(Set.of(ApiKeyScope.READ, ApiKeyScope.WRITE));
    apiKey.setUser(entityUser);

    when(apiKeyService.validateKey("sf_live_bob")).thenReturn(Optional.of(apiKey));
    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername("bob")
            .password("")
            .authorities("ROLE_DEVELOPER")
            .build();
    when(userDetailsService.loadUserByUsername("bob")).thenReturn(userDetails);

    MockHttpServletRequest req = new MockHttpServletRequest("GET", "/mcp/sse");
    req.addHeader("Authorization", "Bearer sf_live_bob");
    MockHttpServletResponse res = new MockHttpServletResponse();

    filter.doFilter(req, res, filterChain);

    assertThat(res.getStatus()).isNotEqualTo(401);
    var authorities = SecurityContextHolder.getContext().getAuthentication().getAuthorities();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isTrue();
    verify(filterChain).doFilter(req, res);
  }
}
