package com.github.farzadsedaghatbin.shipflow.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link GitLabMcpProvider}. Uses a {@link MockWebServer} standing in for a
 * GitLab instance (self-hosted or gitlab.com) since the provider calls the GitLab REST API v4
 * directly rather than a JSON-RPC MCP-server intermediary.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitLabMcpProvider Tests")
class GitLabMcpProviderTest {

  @Mock
  private OrganizationSettingsService settingsService;

  private MockWebServer server;
  private McpConfig mcpConfig;
  private GitLabMcpProvider provider;

  @BeforeEach
  void setUp() throws Exception {
    server = new MockWebServer();
    server.start();

    mcpConfig = new McpConfig();
    mcpConfig.getGitlab().setEnabled(true);
    mcpConfig.getGitlab().setServerUrl(server.url("").toString());

    RestTemplate restTemplate = new RestTemplate();
    provider = new GitLabMcpProvider(mcpConfig, restTemplate, settingsService);
  }

  @AfterEach
  void tearDown() throws Exception {
    server.shutdown();
  }

  @Test
  @DisplayName("getProviderType returns 'gitlab'")
  void getProviderType_returnsGitlab() {
    assertThat(provider.getProviderType()).isEqualTo("gitlab");
  }

  @Nested
  @DisplayName("isAvailable")
  class IsAvailable {

    @Test
    @DisplayName("true when enabled and serverUrl is set")
    void trueWhenConfigured() {
      assertThat(provider.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("false when disabled")
    void falseWhenDisabled() {
      mcpConfig.getGitlab().setEnabled(false);
      assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("false when serverUrl is blank")
    void falseWhenServerUrlBlank() {
      mcpConfig.getGitlab().setServerUrl(" ");
      assertThat(provider.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("false when serverUrl is null")
    void falseWhenServerUrlNull() {
      mcpConfig.getGitlab().setServerUrl(null);
      assertThat(provider.isAvailable()).isFalse();
    }
  }

  @Nested
  @DisplayName("listFiles")
  class ListFiles {

    @Test
    @DisplayName("returns blob paths from the repository tree, skipping directories")
    void returnsBlobPathsOnly() throws Exception {
      when(settingsService.getGitlabAccessToken()).thenReturn("glpat-test-token");
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("[" + "{\"id\":\"1\",\"path\":\"src\",\"type\":\"tree\"},"
              + "{\"id\":\"2\",\"path\":\"src/App.java\",\"type\":\"blob\"},"
              + "{\"id\":\"3\",\"path\":\"README.md\",\"type\":\"blob\"}]"));

      List<String> files = provider.listFiles(Map.of("projectId", "42", "ref", "main"));

      assertThat(files).containsExactly("src/App.java", "README.md");

      RecordedRequest request = server.takeRequest();
      assertThat(request.getPath()).startsWith("/api/v4/projects/42/repository/tree");
      assertThat(request.getPath()).contains("recursive=true");
      assertThat(request.getHeader("PRIVATE-TOKEN")).isEqualTo("glpat-test-token");
    }

    @Test
    @DisplayName("URL-encodes a namespaced project path")
    void encodesNamespacedProjectPath() throws Exception {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse().setHeader("Content-Type", "application/json").setBody("[]"));

      provider.listFiles(Map.of("projectId", "group/sub/project"));

      RecordedRequest request = server.takeRequest();
      assertThat(request.getPath()).contains("/projects/group%2Fsub%2Fproject/repository/tree");
    }

    @Test
    @DisplayName("follows pagination via X-Next-Page header")
    void followsPagination() throws Exception {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setHeader("X-Next-Page", "2")
          .setBody("[{\"path\":\"a.txt\",\"type\":\"blob\"}]"));
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("[{\"path\":\"b.txt\",\"type\":\"blob\"}]"));

      List<String> files = provider.listFiles(Map.of("projectId", "42"));

      assertThat(files).containsExactly("a.txt", "b.txt");
      assertThat(server.getRequestCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("caches results for repeated calls with the same project/ref")
    void cachesResults() throws Exception {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("[{\"path\":\"a.txt\",\"type\":\"blob\"}]"));

      Map<String, String> context = Map.of("projectId", "42", "ref", "main");
      List<String> first = provider.listFiles(context);
      List<String> second = provider.listFiles(context);

      assertThat(first).isEqualTo(second);
      assertThat(server.getRequestCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("returns empty list and makes no request when not available")
    void emptyWhenNotAvailable() {
      mcpConfig.getGitlab().setEnabled(false);
      assertThat(provider.listFiles(Map.of("projectId", "42"))).isEmpty();
      assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("returns empty list and makes no request when projectId missing")
    void emptyWhenProjectIdMissing() {
      assertThat(provider.listFiles(Map.of())).isEmpty();
      assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("returns empty list on HTTP error")
    void emptyOnHttpError() {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"message\":\"404 Project Not Found\"}"));

      assertThat(provider.listFiles(Map.of("projectId", "999"))).isEmpty();
    }
  }

  @Nested
  @DisplayName("readFile")
  class ReadFile {

    @Test
    @DisplayName("returns raw file content")
    void returnsRawContent() throws Exception {
      when(settingsService.getGitlabAccessToken()).thenReturn("glpat-test-token");
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "text/plain")
          .setBody("public class App {}"));

      Optional<String> content = provider.readFile(Map.of("projectId", "42", "ref", "main"), "src/App.java");

      assertThat(content).contains("public class App {}");

      RecordedRequest request = server.takeRequest();
      assertThat(request.getPath())
          .isEqualTo("/api/v4/projects/42/repository/files/src%2FApp.java/raw?ref=main");
      assertThat(request.getHeader("PRIVATE-TOKEN")).isEqualTo("glpat-test-token");
    }

    @Test
    @DisplayName("returns empty on 404")
    void emptyOn404() {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse().setResponseCode(404).setBody("{\"message\":\"404 File Not Found\"}"));

      assertThat(provider.readFile(Map.of("projectId", "42"), "missing.txt")).isEmpty();
    }

    @Test
    @DisplayName("returns empty on 403 without throwing")
    void emptyOn403() {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse().setResponseCode(403).setBody("{\"message\":\"403 Forbidden\"}"));

      assertThat(provider.readFile(Map.of("projectId", "42"), "secret.txt")).isEmpty();
    }

    @Test
    @DisplayName("returns empty when not available")
    void emptyWhenNotAvailable() {
      mcpConfig.getGitlab().setEnabled(false);
      assertThat(provider.readFile(Map.of("projectId", "42"), "a.txt")).isEmpty();
      assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("returns empty when projectId missing")
    void emptyWhenProjectIdMissing() {
      assertThat(provider.readFile(Map.of(), "a.txt")).isEmpty();
      assertThat(server.getRequestCount()).isZero();
    }
  }

  @Nested
  @DisplayName("searchFiles")
  class SearchFiles {

    @Test
    @DisplayName("returns matching paths from blob search results")
    void returnsMatchingPaths() throws Exception {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("[{\"path\":\"src/Foo.java\",\"basename\":\"Foo\"},"
              + "{\"path\":\"src/Bar.java\",\"basename\":\"Bar\"}]"));

      List<String> matches = provider.searchFiles(Map.of("projectId", "42"), "TODO");

      assertThat(matches).containsExactly("src/Foo.java", "src/Bar.java");

      RecordedRequest request = server.takeRequest();
      assertThat(request.getPath()).contains("/projects/42/search?scope=blobs&search=TODO");
    }

    @Test
    @DisplayName("returns empty list when not available")
    void emptyWhenNotAvailable() {
      mcpConfig.getGitlab().setEnabled(false);
      assertThat(provider.searchFiles(Map.of("projectId", "42"), "x")).isEmpty();
      assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("returns empty list when projectId missing")
    void emptyWhenProjectIdMissing() {
      assertThat(provider.searchFiles(Map.of(), "x")).isEmpty();
      assertThat(server.getRequestCount()).isZero();
    }
  }

  @Nested
  @DisplayName("getResourceContext")
  class GetResourceContext {

    @Test
    @DisplayName("returns project attributes bundled with a provider key")
    void returnsProjectAttributes() {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse()
          .setHeader("Content-Type", "application/json")
          .setBody("{\"id\":42,\"name\":\"demo\",\"default_branch\":\"main\"}"));

      Map<String, Object> context = provider.getResourceContext(Map.of("projectId", "42"));

      assertThat(context).containsEntry("provider", "gitlab");
      assertThat(context).containsEntry("name", "demo");
      assertThat(context).containsEntry("default_branch", "main");
    }

    @Test
    @DisplayName("returns empty map when not available")
    void emptyWhenNotAvailable() {
      mcpConfig.getGitlab().setEnabled(false);
      assertThat(provider.getResourceContext(Map.of("projectId", "42"))).isEmpty();
    }

    @Test
    @DisplayName("returns empty map when projectId missing")
    void emptyWhenProjectIdMissing() {
      assertThat(provider.getResourceContext(Map.of())).isEmpty();
      assertThat(server.getRequestCount()).isZero();
    }

    @Test
    @DisplayName("returns empty map on HTTP error")
    void emptyOnHttpError() {
      when(settingsService.getGitlabAccessToken()).thenReturn("token");
      server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

      assertThat(provider.getResourceContext(Map.of("projectId", "42"))).isEmpty();
    }
  }

  @Test
  @DisplayName("cleanupExpiredCache and clearFileListCache do not throw when cache is empty")
  void cacheMaintenanceMethodsAreSafe() {
    provider.cleanupExpiredCache();
    provider.clearFileListCache("42", "main");
  }
}
