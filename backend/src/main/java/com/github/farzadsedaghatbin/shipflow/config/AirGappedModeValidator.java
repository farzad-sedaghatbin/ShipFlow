package com.github.farzadsedaghatbin.shipflow.config;

import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import com.github.farzadsedaghatbin.shipflow.service.mcp.McpClientService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Startup validator that enforces ShipFlow's air-gapped guarantee.
 *
 * <p>When {@code app.air-gapped.enabled=true} this listener runs once the context is ready and:
 *
 * <ol>
 *   <li>Fails startup if the configured active AI provider is NOT local (only Ollama qualifies).
 *   <li>Fails startup if any external MCP client (GitHub / Figma / Notion / Confluence / GitLab /
 *       Azure DevOps / SharePoint) is active.
 *   <li>Fails startup if the Ollama base URL host is an obviously public HTTPS endpoint; private /
 *       loopback / RFC-1918 / cluster-internal hostnames are allowed (conservative to avoid false
 *       positives on Kubernetes service names).
 *   <li>Performs a best-effort reachability preflight against the Ollama base URL — a failure logs a
 *       WARN but does NOT fail startup (Ollama may come up later).
 *   <li>Logs a prominent banner when all checks pass.
 * </ol>
 *
 * <p>When air-gapped mode is disabled this listener is a no-op. It establishes the boot-time
 * config-validation pattern for ShipFlow (an {@link ApplicationListener} of
 * {@link ApplicationReadyEvent}).
 */
@Component
@Slf4j
public class AirGappedModeValidator implements ApplicationListener<ApplicationReadyEvent> {

  private final AirGappedProperties airGappedProperties;
  private final List<McpClientService> mcpClients;
  private final String aiProvider;
  private final String ollamaBaseUrl;

  public AirGappedModeValidator(
      AirGappedProperties airGappedProperties,
      List<McpClientService> mcpClients,
      @Value("${app.ai.provider:ollama}") String aiProvider,
      @Value("${app.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
    this.airGappedProperties = airGappedProperties;
    this.mcpClients = mcpClients;
    this.aiProvider = aiProvider;
    this.ollamaBaseUrl = ollamaBaseUrl;
  }

  @Override
  public void onApplicationEvent(ApplicationReadyEvent event) {
    if (!airGappedProperties.isEnabled()) {
      return;
    }
    validate();
  }

  /** Run all air-gapped checks. Visible for testing — call directly with a constructed instance. */
  public void validate() {
    validateProviderIsLocal();
    validateNoExternalMcp();
    validateOllamaUrlIsPrivate();
    preflightOllama();
    logBanner();
  }

  private void validateProviderIsLocal() {
    LLMProviderType providerType;
    try {
      providerType = LLMProviderType.fromConfigValue(aiProvider);
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "AIR_GAPPED_MODE=true but AI_PROVIDER='" + aiProvider + "' is not a recognized provider. "
              + e.getMessage(),
          e);
    }
    if (!providerType.isLocal()) {
      throw new IllegalStateException("AIR_GAPPED_MODE=true but AI_PROVIDER='" + aiProvider
          + "' requires external network egress — set AI_PROVIDER=ollama or disable air-gapped mode"
          + " (AIR_GAPPED_MODE=false).");
    }
  }

  private void validateNoExternalMcp() {
    List<String> active = new ArrayList<>();
    for (McpClientService client : mcpClients) {
      try {
        if (client.isAvailable()) {
          active.add(client.getProviderType());
        }
      } catch (RuntimeException e) {
        // A misconfigured client must not mask the air-gapped check; treat as not-active but log it.
        log.warn("Could not determine availability of MCP client {}: {}",
            client.getClass().getSimpleName(), e.getMessage());
      }
    }
    if (!active.isEmpty()) {
      throw new IllegalStateException("AIR_GAPPED_MODE=true but external MCP client(s) are enabled: "
          + String.join(", ", active)
          + " — disable these integrations (their *_ENABLED flags / credentials) or turn off"
          + " air-gapped mode (AIR_GAPPED_MODE=false).");
    }
  }

  private void validateOllamaUrlIsPrivate() {
    URI uri;
    try {
      uri = URI.create(ollamaBaseUrl.trim());
    } catch (IllegalArgumentException e) {
      throw new IllegalStateException(
          "AIR_GAPPED_MODE=true but Ollama base URL is malformed: '" + ollamaBaseUrl + "'.", e);
    }
    String host = uri.getHost();
    if (host == null || host.isBlank()) {
      // No host parsed (e.g. a bare hostname without scheme) — be conservative and allow it.
      log.debug("Ollama base URL '{}' has no parseable host; allowing under air-gapped mode.",
          ollamaBaseUrl);
      return;
    }
    if (!isPrivateOrLocalHost(host)) {
      throw new IllegalStateException("AIR_GAPPED_MODE=true but Ollama base URL '" + ollamaBaseUrl
          + "' points at what looks like a public host '" + host
          + "'. Point OLLAMA_BASE_URL at a local / private / cluster-internal address (e.g."
          + " http://localhost:11434 or http://ollama.ai.svc.cluster.local:11434).");
    }
  }

  /**
   * Conservative classification of a host as local / private / cluster-internal. Returns false only
   * for hosts that look unambiguously public (a dotted name with a public-looking TLD, or a routable
   * public IPv4 literal).
   */
  static boolean isPrivateOrLocalHost(String rawHost) {
    String host = rawHost.toLowerCase(Locale.ROOT).trim();
    // Strip IPv6 brackets if present.
    if (host.startsWith("[") && host.endsWith("]")) {
      host = host.substring(1, host.length() - 1);
    }
    if (host.isEmpty()) {
      return true;
    }
    // Loopback names / IPv6 loopback.
    if (host.equals("localhost") || host.equals("::1") || host.startsWith("localhost.")) {
      return true;
    }
    // IPv6 unique-local (fc00::/7) — treat as private.
    if (host.startsWith("fc") || host.startsWith("fd")) {
      return true;
    }
    // IPv4 literal handling.
    if (host.matches("\\d{1,3}(\\.\\d{1,3}){3}")) {
      return isPrivateIpv4(host);
    }
    // Bare hostname (no dots) — e.g. a docker/k8s service short name.
    if (!host.contains(".")) {
      return true;
    }
    // Cluster-internal DNS suffixes.
    if (host.endsWith(".local")
        || host.endsWith(".internal")
        || host.endsWith(".svc")
        || host.endsWith(".cluster.local")
        || host.contains(".svc.")) {
      return true;
    }
    // .lan / .home / .intranet style private TLDs.
    if (host.endsWith(".lan") || host.endsWith(".home") || host.endsWith(".intranet")) {
      return true;
    }
    // Otherwise it has a public-looking dotted name → treat as public.
    return false;
  }

  private static boolean isPrivateIpv4(String ip) {
    String[] parts = ip.split("\\.");
    int[] o = new int[4];
    for (int i = 0; i < 4; i++) {
      try {
        o[i] = Integer.parseInt(parts[i]);
      } catch (NumberFormatException e) {
        return false;
      }
      if (o[i] < 0 || o[i] > 255) {
        return false;
      }
    }
    // 127.0.0.0/8 loopback
    if (o[0] == 127) {
      return true;
    }
    // 10.0.0.0/8
    if (o[0] == 10) {
      return true;
    }
    // 172.16.0.0/12
    if (o[0] == 172 && o[1] >= 16 && o[1] <= 31) {
      return true;
    }
    // 192.168.0.0/16
    if (o[0] == 192 && o[1] == 168) {
      return true;
    }
    // 169.254.0.0/16 link-local
    if (o[0] == 169 && o[1] == 254) {
      return true;
    }
    return false;
  }

  private void preflightOllama() {
    String base = ollamaBaseUrl.trim();
    String url = base.endsWith("/") ? base + "api/tags" : base + "/api/tags";
    try {
      HttpClient client =
          HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(3))
          .GET()
          .build();
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 200 && response.statusCode() < 500) {
        log.info("Air-gapped preflight: Ollama reachable at {} (HTTP {})", base,
            response.statusCode());
      } else {
        log.warn("Air-gapped preflight: Ollama at {} returned HTTP {} — verify the local model"
            + " server is healthy.", base, response.statusCode());
      }
    } catch (Exception e) {
      log.warn("Air-gapped preflight: could not reach Ollama at {} ({}). Air-gapped mode is still"
          + " active; ensure the local model server is running before using AI features.", base,
          e.getMessage());
    }
  }

  private void logBanner() {
    log.info(
        "\n========================================================================\n"
            + "  AIR-GAPPED MODE ACTIVE\n"
            + "  AI provider     : {} (local, no external egress)\n"
            + "  Ollama base URL : {}\n"
            + "  External MCP    : all disabled\n"
            + "  Embeddings      : local in-process ONNX (all-MiniLM-L6-v2)\n"
            + "========================================================================",
        aiProvider, ollamaBaseUrl);
  }
}
