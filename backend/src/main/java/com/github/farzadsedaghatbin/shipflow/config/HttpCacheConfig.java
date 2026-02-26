package com.github.farzadsedaghatbin.shipflow.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.filter.ShallowEtagHeaderFilter;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * HTTP cache configuration.
 *
 * <p>Registers two filters for {@code /api/*}:
 * <ol>
 *   <li><b>ShallowEtagHeaderFilter</b> – computes an {@code ETag} (MD5 of the response body)
 *       for every response and returns {@code 304 Not Modified} when the client sends a matching
 *       {@code If-None-Match} header. This eliminates redundant body transfers for unchanged data.</li>
 *   <li><b>CacheControlFilter</b> – sets {@code Cache-Control: no-cache, must-revalidate} on GET
 *       responses so that clients always revalidate with the server (rather than serving stale data
 *       from the browser's disk/memory cache), while {@code must-revalidate} ensures proxies also
 *       revalidate rather than serving stale content when disconnected from the origin.</li>
 * </ol>
 */
@Configuration
public class HttpCacheConfig {

    /**
     * Static asset cache configuration — three tiers:
     *
     * <ul>
     *   <li><b>/assets/**</b> — Vite content-hashed JS/CSS bundles. Safe to cache for 1 year
     *       with {@code immutable}: the filename changes whenever the content changes, so the
     *       browser never needs to revalidate.</li>
     *   <li><b>Favicons, icons, manifest</b> — Infrequently changed brand assets (PNG, SVG, ICO,
     *       webmanifest, txt). Cached publicly for 7 days so repeat visits skip the network.</li>
     *   <li><b>/index.html</b> — The SPA entry point must always be revalidated so users
     *       receive the latest deploy without a hard refresh.</li>
     * </ul>
     */
    @Bean
    public WebMvcConfigurer staticResourceCacheConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                // Vite-built bundles carry a content hash — safe to cache forever
                registry.addResourceHandler("/assets/**")
                        .addResourceLocations("classpath:/static/assets/")
                        .setCacheControl(
                                CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable());

                // Favicons, icons, webmanifest, robots.txt — 7-day public cache
                registry.addResourceHandler("/*.png", "/*.ico", "/*.svg",
                                "/*.webmanifest", "/*.txt")
                        .addResourceLocations("classpath:/static/")
                        .setCacheControl(
                                CacheControl.maxAge(7, TimeUnit.DAYS).cachePublic());

                // index.html — always revalidate so fresh deployments are transparent
                registry.addResourceHandler("/", "/index.html")
                        .addResourceLocations("classpath:/static/")
                        .setCacheControl(CacheControl.noCache().mustRevalidate());
            }
        };
    }

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
     * Sets Cache-Control: no-cache, must-revalidate on all GET /api/* responses.
     * This tells the browser "always revalidate" — combined with ETag,
     * the browser will send If-None-Match and get a fast 304 when nothing changed.
     * The must-revalidate directive additionally prevents proxies from serving
     * stale content when they cannot reach the origin server.
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
                    response.setHeader("Cache-Control", "no-cache, must-revalidate");
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
