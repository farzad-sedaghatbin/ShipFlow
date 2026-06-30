package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.config.AirGappedProperties;
import com.github.farzadsedaghatbin.shipflow.config.llm.LLMProviderType;
import com.github.farzadsedaghatbin.shipflow.dto.system.AirGappedStatusDTO;
import com.github.farzadsedaghatbin.shipflow.service.mcp.McpClientService;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Read-only system-status queries surfaced to the UI. Currently exposes the
 * air-gapped mode state so the frontend can render an indicator and admins can
 * confirm enforcement.
 *
 * <p>This sits in the service layer (per the Controller → Service → Repository
 * discipline) so the controller never inspects config or performs network I/O
 * directly.
 */
@Service
@Slf4j
public class SystemStatusService {

  private final AirGappedProperties airGappedProperties;
  private final List<McpClientService> mcpClients;
  private final String aiProvider;
  private final String ollamaBaseUrl;

  public SystemStatusService(
      AirGappedProperties airGappedProperties,
      List<McpClientService> mcpClients,
      @Value("${app.ai.provider:ollama}") String aiProvider,
      @Value("${app.ai.ollama.base-url:http://localhost:11434}") String ollamaBaseUrl) {
    this.airGappedProperties = airGappedProperties;
    this.mcpClients = mcpClients;
    this.aiProvider = aiProvider;
    this.ollamaBaseUrl = ollamaBaseUrl;
  }

  /**
   * Build the current air-gapped status. When air-gapped mode is enabled the
   * external-MCP list is reported as empty (enforcement guarantees none are
   * active); otherwise it reflects the actually-active external MCP clients.
   *
   * @return the current air-gapped status
   */
  public AirGappedStatusDTO getAirGappedStatus() {
    boolean enabled = airGappedProperties.isEnabled();

    boolean providerLocal = false;
    try {
      providerLocal = LLMProviderType.fromConfigValue(aiProvider).isLocal();
    } catch (IllegalArgumentException e) {
      log.debug("Unknown AI provider '{}' while building air-gapped status: {}", aiProvider,
          e.getMessage());
    }

    List<String> externalMcp = enabled ? List.of() : activeExternalMcp();
    boolean reachable = ollamaReachable();

    return new AirGappedStatusDTO(
        enabled, aiProvider, providerLocal, ollamaBaseUrl, reachable, externalMcp);
  }

  private List<String> activeExternalMcp() {
    List<String> active = new ArrayList<>();
    for (McpClientService client : mcpClients) {
      try {
        if (client.isAvailable()) {
          active.add(client.getProviderType());
        }
      } catch (RuntimeException e) {
        log.debug("Could not determine availability of MCP client {}: {}",
            client.getClass().getSimpleName(), e.getMessage());
      }
    }
    return active;
  }

  private boolean ollamaReachable() {
    String base = ollamaBaseUrl == null ? "" : ollamaBaseUrl.trim();
    if (base.isEmpty()) {
      return false;
    }
    String url = base.endsWith("/") ? base + "api/tags" : base + "/api/tags";
    try {
      HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(URI.create(url))
          .timeout(Duration.ofSeconds(3))
          .GET()
          .build();
      HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
      return response.statusCode() >= 200 && response.statusCode() < 500;
    } catch (Exception e) {
      return false;
    }
  }
}
