package com.github.farzadsedaghatbin.shipflow.security;

import jakarta.servlet.DispatcherType;
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
        .csrf(AbstractHttpConfigurer::disable)
        .headers(headers -> headers
            .frameOptions(frame -> frame.sameOrigin())
            .contentTypeOptions(cto -> {})
            .contentSecurityPolicy(csp -> csp.policyDirectives(
                "default-src 'self'; "
                + "script-src 'self' 'unsafe-inline'; "
                + "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; "
                + "img-src 'self' data: blob: https:; "
                + "font-src 'self' data: https://fonts.gstatic.com; "
                + "connect-src 'self' ws: wss: https:; "
                + "frame-ancestors 'self'; "
                + "object-src 'none'; "
                + "base-uri 'self'"))
            .httpStrictTransportSecurity(hsts -> hsts
                .maxAgeInSeconds(31536000)
                .includeSubDomains(true))
            .referrerPolicy(rp -> rp.policy(
                org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            .permissionsPolicy(pp -> pp.policy(
                "camera=(), microphone=(), geolocation=(), payment=(), usb=()")))
        .exceptionHandling(ex -> ex.authenticationEntryPoint(jwtAuthenticationEntryPoint)
            .accessDeniedHandler(jwtAccessDeniedHandler))
        .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth
            .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR).permitAll()
            .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/login", "/welcome", "/dashboard", "/dashboard/**", "/cycles/**",
                "/pitches/**", "/betting/**", "/projects/**", "/health/**", "/qa/**", "/reports/**",
                "/tests/**", "/teams/**", "/people/**", "/users/**", "/profile/**", "/worklogs/**",
                "/my-worklogs/**", "/meetings/**", "/tasks/**")
            .permitAll()
            .requestMatchers("/api/auth/**").permitAll().requestMatchers("/api/public/**").permitAll()
            .requestMatchers("/api/v1/public/**").permitAll()
            .requestMatchers("/api/sso/**").permitAll()
            .requestMatchers("/api/inbound/**").permitAll()
            .requestMatchers("/api/qa/status").permitAll()
            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
            .requestMatchers("/h2-console/**").permitAll()
            .requestMatchers("/actuator/health").permitAll()
            .requestMatchers("/api/**").authenticated()
            .requestMatchers("/mcp/health").permitAll()
            .requestMatchers("/mcp/**").permitAll()
            .anyRequest().permitAll())
        .authenticationProvider(authenticationProvider())
        .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(maliciousHeaderFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterBefore(mcpAuthFilter, UsernamePasswordAuthenticationFilter.class)
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
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .toList();
        List<String> exactOrigins = origins.stream()
                .filter(o -> !o.contains("*"))
                .toList();
        List<String> patternOrigins = origins.stream()
                .filter(o -> o.contains("*"))
                .toList();
        if (!exactOrigins.isEmpty()) {
            config.setAllowedOrigins(exactOrigins);
        }
        if (!patternOrigins.isEmpty()) {
            config.setAllowedOriginPatterns(patternOrigins);
        }
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setExposedHeaders(List.of("Authorization"));

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
