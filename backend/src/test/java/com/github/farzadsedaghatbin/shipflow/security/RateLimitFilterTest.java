package com.github.farzadsedaghatbin.shipflow.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Unit tests for {@link RateLimitFilter}.
 *
 * <p>Verifies that rate limits are enforced on targeted paths and that
 * unrelated paths bypass the filter without consuming tokens.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitFilterTest {

  private RateLimitFilter filter;

  @Mock
  private HttpServletRequest request;

  @Mock
  private HttpServletResponse response;

  @Mock
  private FilterChain filterChain;

  private StringWriter responseWriter;

  @BeforeEach
  void setUp() throws Exception {
    filter = new RateLimitFilter();
    responseWriter = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
  }

  @Test
  void unratedPath_alwaysPassesThrough() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/projects/1/tasks");

    // Should pass through many times without triggering a 429
    for (int i = 0; i < 50; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(50)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void authPath_allowsUpToCapacityPerIp() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");

    // First 10 requests should be allowed (capacity = 10)
    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(10)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void authPath_blocksAfterCapacityExceeded() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");

    // Exhaust the bucket (capacity 10)
    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    // 11th request must be rate-limited
    filter.doFilterInternal(request, response, filterChain);

    // filterChain called exactly 10 times (not 11)
    verify(filterChain, times(10)).doFilter(request, response);
    verify(response).setStatus(429);
    verify(response).setHeader("Retry-After", "60");

    String body = responseWriter.toString();
    assertTrue(body.contains("Too many requests"), "Body should contain 'Too many requests'");
    assertTrue(body.contains("retryAfter"), "Body should contain 'retryAfter'");
  }

  @Test
  void searchPath_allowsUpTo30PerIp() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/search?q=foo");

    for (int i = 0; i < 30; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(30)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void searchPath_blocksAfter30Requests() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/search?q=foo");

    for (int i = 0; i < 30; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    // 31st should be rejected
    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(30)).doFilter(request, response);
    verify(response).setStatus(429);
  }

  @Test
  void aiPath_wiseArchitecture_allowsUpTo5PerIp() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/wise-architecture/analyze");

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(5)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void aiPath_wiseArchitecture_blocksAfter5Requests() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/wise-architecture/analyze");

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(5)).doFilter(request, response);
    verify(response).setStatus(429);
  }

  @Test
  void aiPath_risk_blocksAfter5Requests() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/risk/analyze");

    for (int i = 0; i < 5; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(5)).doFilter(request, response);
    verify(response).setStatus(429);
  }

  @Test
  void differentIps_haveIndependentBuckets() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");

    HttpServletRequest request2 = mock(HttpServletRequest.class);
    when(request2.getRequestURI()).thenReturn("/api/auth/login");
    when(request2.getRemoteAddr()).thenReturn("10.0.0.2");
    when(request2.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request2.getHeader("X-Real-IP")).thenReturn(null);

    // Exhaust IP 1 bucket
    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }
    filter.doFilterInternal(request, response, filterChain); // 429

    // IP 2 should still be allowed (independent bucket)
    filter.doFilterInternal(request2, response, filterChain);

    // request1: 10 passes + 1 block; request2: 1 pass — filterChain called 11 times total
    verify(filterChain, times(11)).doFilter(any(), any());
  }

  @Test
  void xForwardedFor_usedAsClientIp() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");
    when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.99, 10.0.0.1");

    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(10)).doFilter(request, response);
  }
}
