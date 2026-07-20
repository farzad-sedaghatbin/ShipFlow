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
  void pathToken_validKeyWithWriteScope_capsToReadOnly() throws Exception {
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
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isFalse();
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_ADMIN"))).isFalse();
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
    assertThat(authorities.stream().anyMatch(a -> a.getAuthority().equals("SCOPE_WRITE"))).isFalse();
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
}
