package com.github.farzadsedaghatbin.shipflow.config;

import java.io.IOException;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Configuration to serve the React SPA from Spring Boot. All non-API routes will be forwarded to
 * index.html for client-side routing.
 */
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    registry
        .addResourceHandler("/**")
        .addResourceLocations("classpath:/static/")
        .resourceChain(true)
        .addResolver(
            new PathResourceResolver() {
              @Override
              protected Resource getResource(String resourcePath, Resource location)
                  throws IOException {
                Resource requestedResource = location.createRelative(resourcePath);

                // If the requested resource exists and is readable, return it
                if (requestedResource.exists() && requestedResource.isReadable()) {
                  return requestedResource;
                }

                // Otherwise, return index.html for SPA routing
                // But not for API calls
                if (!resourcePath.startsWith("api/")) {
                  return new ClassPathResource("/static/index.html");
                }

                return null;
              }
            });
  }
}
