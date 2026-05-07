package com.github.farzadsedaghatbin.shipflow.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI shipFlowOpenAPI() {
    return new OpenAPI().info(new Info().title("ShipFlow API")
        .description("REST API for Shape Up methodology analytics and tracking").version("1.0.0")
        .contact(new Contact().name("Farzad Sedaghatbin").url("https://github.com/farzad-sedaghatbin/ShipFlow"))
        .license(new License().name("MIT License").url("https://opensource.org/licenses/MIT")));
  }
}
