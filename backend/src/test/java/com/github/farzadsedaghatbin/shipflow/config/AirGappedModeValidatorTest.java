package com.github.farzadsedaghatbin.shipflow.config;

import static org.junit.jupiter.api.Assertions.*;

import com.github.farzadsedaghatbin.shipflow.service.mcp.McpClientService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * Lightweight unit tests for the air-gapped startup validator. Constructed
 * directly with stub collaborators — no Spring context required.
 */
class AirGappedModeValidatorTest {

  private static AirGappedProperties props(boolean enabled) {
    AirGappedProperties p = new AirGappedProperties();
    p.setEnabled(enabled);
    return p;
  }

  /** A stub MCP client whose availability and type are fixed at construction. */
  private static final class StubMcpClient implements McpClientService {
    private final boolean available;
    private final String type;

    StubMcpClient(String type, boolean available) {
      this.type = type;
      this.available = available;
    }

    @Override
    public boolean isAvailable() {
      return available;
    }

    @Override
    public String getProviderType() {
      return type;
    }

    @Override
    public List<String> listFiles(Map<String, String> context) {
      return List.of();
    }

    @Override
    public Optional<String> readFile(Map<String, String> context, String filePath) {
      return Optional.empty();
    }

    @Override
    public List<String> searchFiles(Map<String, String> context, String pattern) {
      return List.of();
    }

    @Override
    public Map<String, Object> getResourceContext(Map<String, String> context) {
      return Map.of();
    }
  }

  @Test
  void enabled_withNonLocalProvider_throws() {
    AirGappedModeValidator validator = new AirGappedModeValidator(
        props(true), List.of(), "openai", "http://localhost:11434");

    IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
    assertTrue(ex.getMessage().contains("openai"));
    assertTrue(ex.getMessage().contains("AIR_GAPPED_MODE"));
  }

  @Test
  void enabled_withLocalProviderAndNoExternalMcp_passes() {
    AirGappedModeValidator validator = new AirGappedModeValidator(
        props(true), List.of(new StubMcpClient("github", false)), "ollama",
        "http://localhost:11434");

    // No exception thrown; the Ollama preflight only WARNs when unreachable.
    assertDoesNotThrow(validator::validate);
  }

  @Test
  void enabled_withActiveExternalMcp_throws() {
    AirGappedModeValidator validator = new AirGappedModeValidator(
        props(true), List.of(new StubMcpClient("github", true)), "ollama",
        "http://localhost:11434");

    IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
    assertTrue(ex.getMessage().contains("github"));
    assertTrue(ex.getMessage().toLowerCase().contains("mcp"));
  }

  @Test
  void enabled_withPublicOllamaUrl_throws() {
    AirGappedModeValidator validator = new AirGappedModeValidator(
        props(true), List.of(), "ollama", "https://ollama.example.com");

    IllegalStateException ex = assertThrows(IllegalStateException.class, validator::validate);
    assertTrue(ex.getMessage().contains("public"));
  }

  @Test
  void disabled_isNoOpEvenWithCloudProvider() {
    AirGappedModeValidator validator = new AirGappedModeValidator(
        props(false), List.of(new StubMcpClient("github", true)), "openai",
        "https://ollama.example.com");

    // onApplicationEvent short-circuits when disabled; validate() is never reached.
    assertDoesNotThrow(() -> validator.onApplicationEvent(null));
  }

  @Test
  void isPrivateOrLocalHost_classifiesCommonHosts() {
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("localhost"));
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("127.0.0.1"));
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("10.1.2.3"));
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("172.16.0.1"));
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("192.168.1.50"));
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("ollama"));
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("ollama.ai.svc.cluster.local"));
    assertTrue(AirGappedModeValidator.isPrivateOrLocalHost("::1"));

    assertFalse(AirGappedModeValidator.isPrivateOrLocalHost("ollama.example.com"));
    assertFalse(AirGappedModeValidator.isPrivateOrLocalHost("8.8.8.8"));
    assertFalse(AirGappedModeValidator.isPrivateOrLocalHost("api.openai.com"));
  }
}
