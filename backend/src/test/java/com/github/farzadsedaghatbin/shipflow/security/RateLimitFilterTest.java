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
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitFilterTest {

  private RateLimitFilter filter;

  @Mock private HttpServletRequest request;
  @Mock private HttpServletResponse response;
  @Mock private FilterChain filterChain;

  private StringWriter responseWriter;

  @BeforeEach
  void setUp() throws Exception {
    filter = new RateLimitFilter();
    ReflectionTestUtils.setField(filter, "authCapacity", 10);
    ReflectionTestUtils.setField(filter, "aiCapacity", 20);
    ReflectionTestUtils.setField(filter, "riskReadCapacity", 60);
    ReflectionTestUtils.setField(filter, "searchCapacity", 30);
    ReflectionTestUtils.setField(filter, "pollCapacity", 120);
    ReflectionTestUtils.setField(filter, "publicApiCapacity", 100);
    responseWriter = new StringWriter();
    when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    when(request.getRemoteAddr()).thenReturn("10.0.0.1");
    when(request.getHeader("X-Forwarded-For")).thenReturn(null);
    when(request.getHeader("X-Real-IP")).thenReturn(null);
  }

  @Test
  void unratedPath_alwaysPassesThrough() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/projects/1/tasks");

    for (int i = 0; i < 50; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(50)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void authPath_allowsUpToCapacityPerIp() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");

    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(10)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void authPath_blocksAfterCapacityExceeded() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");

    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(10)).doFilter(request, response);
    verify(response).setStatus(429);

    String body = responseWriter.toString();
    assertTrue(body.contains("Too many requests"));
    assertTrue(body.contains("retryAfter"));
  }

  @Test
  void searchPath_allowsUpToCapacity() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/search?q=foo");

    for (int i = 0; i < 30; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(30)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void searchPath_blocksAfterCapacity() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/search?q=foo");

    for (int i = 0; i < 30; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(30)).doFilter(request, response);
    verify(response).setStatus(429);
  }

  @Test
  void aiPath_wiseArchitecture_allowsUpToCapacity() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/wise-architecture/some-endpoint");

    for (int i = 0; i < 20; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(20)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void aiPath_wiseArchitecture_blocksAfterCapacity() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/wise-architecture/some-endpoint");

    for (int i = 0; i < 20; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(20)).doFilter(request, response);
    verify(response).setStatus(429);
  }

  @Test
  void riskAnalyze_usesAiBucket() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/risk/async/pitch/1/analyze");

    for (int i = 0; i < 20; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(20)).doFilter(request, response);
    verify(response, never()).setStatus(429);

    filter.doFilterInternal(request, response, filterChain);
    verify(response).setStatus(429);
  }

  @Test
  void riskAsk_usesAiBucket() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/risk/pitch/1/ask");

    for (int i = 0; i < 20; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(20)).doFilter(request, response);
    verify(response, never()).setStatus(429);

    filter.doFilterInternal(request, response, filterChain);
    verify(response).setStatus(429);
  }

  @Test
  void riskHistory_usesRiskReadBucket() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/risk/pitch/1/history");

    for (int i = 0; i < 60; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(60)).doFilter(request, response);
    verify(response, never()).setStatus(429);

    filter.doFilterInternal(request, response, filterChain);
    verify(response).setStatus(429);
  }

  @Test
  void riskStatus_usesRiskReadBucket() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/risk/status");

    for (int i = 0; i < 60; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(60)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void riskAnalyzeAndAsk_shareAiBucket() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/risk/async/pitch/1/analyze");
    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    when(request.getRequestURI()).thenReturn("/api/risk/pitch/2/ask");
    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(20)).doFilter(request, response);
    verify(response, never()).setStatus(429);

    filter.doFilterInternal(request, response, filterChain);
    verify(response).setStatus(429);
  }

  @Test
  void asyncJobPolling_usesHighCapacityPollBucket() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/risk/async/jobs/abc-123/status");

    for (int i = 0; i < 120; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(120)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void publicApiPath_allowsUpToCapacity() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/v1/public/pitches");

    for (int i = 0; i < 100; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(100)).doFilter(request, response);
    verify(response, never()).setStatus(429);
  }

  @Test
  void publicApiPath_blocksAfterCapacityExceeded() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/v1/public/pitches");

    for (int i = 0; i < 100; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    filter.doFilterInternal(request, response, filterChain);

    verify(filterChain, times(100)).doFilter(request, response);
    verify(response).setStatus(429);
  }

  @Test
  void publicApiDataPath_usesSamePublicApiBucket() throws Exception {
    // DataManagementController is mounted under /api/v1/public/data/** — same prefix, same bucket.
    when(request.getRequestURI()).thenReturn("/api/v1/public/pitches");
    for (int i = 0; i < 50; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    when(request.getRequestURI()).thenReturn("/api/v1/public/data/export/1/json");
    for (int i = 0; i < 50; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    verify(filterChain, times(100)).doFilter(request, response);
    verify(response, never()).setStatus(429);

    filter.doFilterInternal(request, response, filterChain);
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

    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }
    filter.doFilterInternal(request, response, filterChain);

    filter.doFilterInternal(request2, response, filterChain);

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

  @Test
  void retryAfterHeader_isSetDynamically() throws Exception {
    when(request.getRequestURI()).thenReturn("/api/auth/login");

    for (int i = 0; i < 10; i++) {
      filter.doFilterInternal(request, response, filterChain);
    }

    filter.doFilterInternal(request, response, filterChain);

    verify(response).setStatus(429);
    verify(response).setHeader(eq("Retry-After"), argThat(val -> {
      long seconds = Long.parseLong(val);
      return seconds > 0 && seconds <= 61;
    }));
  }
}
