package com.github.farzadsedaghatbin.shipflow.security;

import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

  private final CustomUserDetailsService userDetailsService;
  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;
  private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
  private final MaliciousHeaderFilter maliciousHeaderFilter;
  private final RateLimitFilter rateLimitFilter;
  private final ApiKeyAuthenticationFilter apiKeyAuthenticationFilter;
  private final McpAuthFilter mcpAuthFilter;

  @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:*,http://127.0.0.1:*}")
  private String allowedOrigins;

  /**
   * Bypass Spring Security completely for static resources. This prevents CORS
   * issues with module scripts.
   */
  @Bean
  public WebSecurityCustomizer webSecurityCustomizer() {
    return (web) -> web.ignoring().requestMatchers("/", "/index.html", "/favicon.ico", "/favicon.svg")
        .requestMatchers("/assets/**").requestMatchers("/*.js", "/*.css", "/*.png", "/*.svg", "/*.ico",
            "/*.json", "/*.woff", "/*.woff2", "/*.ttf");
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
        // CSRF protection is safely disabled because:
        // 1. This is a stateless REST API using JWT tokens (not cookies)
        // 2. SessionCreationPolicy.STATELESS means no server-side sessions
        // 3. JWT tokens are sent via Authorization header, not vulnerable to CSRF
        // See:
        // https://security.stackexchange.com/questions/170388/do-i-need-csrf-token-if-im-using-bearer-jwt
        .csrf(AbstractHttpConfigurer::disable)
        // Security response headers
        .headers(headers -> headers
            // Prevent clickjacking — only allow framing from same origin
            .frameOptions(frame -> frame.sameOrigin())
            // Prevent MIME-type sniffing
            .contentTypeOptions(cto -> {})
            // Content Security Policy for the React SPA
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; "
                + "script-src 'self' 'unsafe-inline'; "
                + "style-src 'self' 'unsafe-inline'; "
                + "img-src 'self' data: blob: https:; "
                + "font-src 'self' data:; "
                + "connect-src 'self' ws: wss: https:; "
                + "frame-ancestors 'self'; "
                + "object-src 'none'; "
                + "base-uri 'self'"))
            // Tell browsers to use HTTPS only (1 year)
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true))
            // Referrer policy
            .referrerPolicy(rp -> rp.policy(
                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            // Permissions policy — restrict sensitive browser features
            .permissionsPolicy(pp -> pp.policy(
                "camera=(), microphone=(), geolocation=(), payment=(), usb=()")))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(jwtAccessDeniedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            // Allow CORS preflight requests
            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
            // SPA routes - all frontend routes should be accessible (React handles auth)
            .requestMatchers("/login", "/welcome", "/dashboard", "/dashboard/**", "/cycles/**",
                "/pitches/**", "/betting/**", "/projects/**", "/health/**", "/qa/**", "/reports/**",
                "/tests/**", "/teams/**", "/people/**", "/users/**", "/profile/**", "/worklogs/**",
                "/my-worklogs/**", "/meetings/**", "/tasks/**")
            .permitAll()
            // Public API endpoints
            .requestMatchers("/api/auth/**").permitAll().requestMatchers("/api/public/**").permitAll()
            // Public REST API v1 (authenticated via X-API-Key header)
            .requestMatchers("/api/v1/public/**").permitAll()
            // Inbound webhooks (each handler validates its own signature)
            .requestMatchers("/api/inbound/**").permitAll()
            // Q&A status endpoint (public to check if feature is enabled)
            .requestMatchers("/api/qa/status").permitAll()
            // Swagger/OpenAPI endpoints
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
            // H2 console (dev only)
            .requestMatchers("/h2-console/**").permitAll()
            // Actuator health endpoint
            .requestMatchers("/actuator/health").permitAll()
            // All API endpoints require authentication
            .requestMatchers("/api/**").authenticated()
            // MCP health is public; all other /mcp/** auth is handled by McpAuthFilter
            .requestMatchers("/mcp/health").permitAll()
            .requestMatchers("/mcp/**").permitAll()
            // Allow any other request (frontend will handle auth)
            .anyRequest().permitAll())
        .authenticationProvider(authenticationProvider())
        // Rate limiting runs first on auth/search/AI paths
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        // Malicious header detection runs after rate limiting
        .addFilterBefore(maliciousHeaderFilter, UsernamePasswordAuthenticationFilter.class)
        // Add MCP API key filter for /mcp/** paths
        .addFilterBefore(mcpAuthFilter, UsernamePasswordAuthenticationFilter.class)
        // Add API key filter before JWT filter for /api/v1/public/** paths
        .addFilterBefore(apiKeyAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
  }

  @Bean
  public AuthenticationProvider authenticationProvider() {
    DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
    authProvider.setUserDetailsService(userDetailsService);
    authProvider.setPasswordEncoder(passwordEncoder());
    return authProvider;
  }

  @Bean
  public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
    return config.getAuthenticationManager();
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowCredentials(true);
    // Parse comma-separated origins from config and trim whitespace
    List<String> origins = Arrays.stream(allowedOrigins.split(",")).map(String::trim).toList();
    // Use both setAllowedOrigins (exact match) and setAllowedOriginPatterns
    // (patterns)
    config.setAllowedOrigins(origins);
    config.setAllowedOriginPatterns(List.of("http://localhost:*", "http://127.0.0.1:*"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
