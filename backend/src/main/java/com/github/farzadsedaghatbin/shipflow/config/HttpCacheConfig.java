package com.github.farzadsedaghatbin.shipflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * HTTP cache configuration.
 *
 * <p>Registers two filters for {@code /api/*}:
 * <ol>
 *   <li><b>ShallowEtagHeaderFilter</b> – computes an {@code ETag} (MD5 of the response body)
 *       for every response and returns {@code 304 Not Modified} when the client sends a matching
 *       {@code If-None-Match} header. This eliminates redundant body transfers for unchanged data.</li>
 *   <li><b>CacheControlFilter</b> – sets {@code Cache-Control: no-cache} on GET responses so that
 *       clients always revalidate with the server (rather than serving stale data from the
 *       browser's disk/memory cache), while still benefiting from 304 responses.</li>
 * </ol>
 */
@Configuration
public class HttpCacheConfig {

    /**
     * Shallow ETag filter – computes ETag from response body and handles If-None-Match → 304.
     * Scoped to /api/* so static assets (index.html, JS bundles) are not affected.
     */
    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new ShallowEtagHeaderFilter());
        registration.addUrlPatterns("/api/*");
        registration.setName("etagFilter");
        registration.setOrder(1);
        return registration;
    }

    /**
     * Sets Cache-Control: no-cache on all GET /api/* responses.
     * This tells the browser "always revalidate" — combined with ETag,
     * the browser will send If-None-Match and get a fast 304 when nothing changed.
     */
    @Bean
    public FilterRegistrationBean<OncePerRequestFilter> cacheControlFilter() {
        FilterRegistrationBean<OncePerRequestFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                    FilterChain filterChain) throws ServletException, IOException {
                filterChain.doFilter(request, response);
                if ("GET".equalsIgnoreCase(request.getMethod()) && !response.containsHeader("Cache-Control")) {
                    response.setHeader("Cache-Control", "no-cache");
                }
            }
        });
        registration.addUrlPatterns("/api/*");
        registration.setName("cacheControlFilter");
        // Run AFTER the ETag filter so the ETag header is already set
        registration.setOrder(2);
        return registration;
    }
}
