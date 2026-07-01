package com.github.farzadsedaghatbin.shipflow.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for ShipFlow's air-gapped AI mode.
 *
 * <p>When {@code enabled} is true, ShipFlow guarantees that its AI / integration stack performs NO
 * external network egress: only a local LLM (Ollama) is permitted and every external MCP client
 * (GitHub, Figma, Notion, Confluence, SharePoint) must be turned off. The guarantee is enforced at
 * two points:
 *
 * <ul>
 *   <li>Startup — {@code AirGappedModeValidator} fails fast if the configured AI provider is not
 *       local or any external MCP client is active.
 *   <li>Runtime — {@code LLMProviderFactory} refuses to build a non-local model so an admin cannot
 *       switch to a cloud provider after boot.
 * </ul>
 *
 * <p>Environment variable equivalent: {@code AIR_GAPPED_MODE=true}.
 */
@Configuration
@ConfigurationProperties(prefix = "app.air-gapped")
@Getter
@Setter
public class AirGappedProperties {

  /** Whether air-gapped mode is enabled. Default: false. */
  private boolean enabled = false;
}
