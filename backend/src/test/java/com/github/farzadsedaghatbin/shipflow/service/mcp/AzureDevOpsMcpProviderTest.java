package com.github.farzadsedaghatbin.shipflow.service.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.service.OrganizationSettingsService;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

/**
 * Unit tests for {@link AzureDevOpsMcpProvider}, mirroring the MockWebServer approach used
 * elsewhere in the test suite (e.g. {@code UrlProviderTest}, {@code TeamsIntegrationRealRequestTest}).
 * No dedicated GitHubMcpProvider/ConfluenceMcpProvider tests exist in this codebase to mirror
 * directly (they're currently exercised only indirectly, via WiseArchitectureServiceTest mocks),
 * so this test talks to a real embedded HTTP server to validate the JSON-RPC request shape,
 * Basic-auth header, and response-parsing/fallback behavior end to end.
 */
class AzureDevOpsMcpProviderTest {

  private static final ObjectMapper JSON = new ObjectMapper();

  private MockWebServer server;
  private OrganizationSettingsService settingsService;
  private AzureDevOpsMcpProvider provider;
  private McpConfig mcpConfig;

  @BeforeEach
  void setUp() throws IOException {
    server = new MockWebServer();
    server.start();

    mcpConfig = new McpConfig();
    mcpConfig.getAzureDevOps().setEnabled(true);
    String base = server.url("").toString();
    mcpConfig.getAzureDevOps().setServerUrl(base.substring(0, base.length() - 1));

    settingsService = mock(OrganizationSettingsService.class);
    when(settingsService.getAzureDevOpsAccessToken()).thenReturn("fake-pat");

    provider = new AzureDevOpsMcpProvider(mcpConfig, new RestTemplate(), settingsService);
  }

  @AfterEach
  void tearDown() throws IOException {
    server.shutdown();
  }

  private Map<String, String> context() {
    Map<String, String> ctx = new LinkedHashMap<>();
    ctx.put("organization", "kixy");
    ctx.put("project", "ShipFlow");
    ctx.put("repository", "shipflow-backend");
    return ctx;
  }

  private String jsonRpcResultResponse(String text) throws Exception {
    Map<String, Object> content = new LinkedHashMap<>();
    content.put("type", "text");
    content.put("text", text);
    Map<String, Object> result = Map.of("content", List.of(content));
    Map<String, Object> body = Map.of("jsonrpc", "2.0", "id", "1", "result", result);
    return JSON.writeValueAsString(body);
  }

  // -----------------------------------------------------------------------
  // isAvailable / getProviderType
  // -----------------------------------------------------------------------

  @Test
  void isAvailable_false_whenDisabled() {
    mcpConfig.getAzureDevOps().setEnabled(false);
    assertThat(provider.isAvailable()).isFalse();
  }

  @Test
  void isAvailable_false_whenNoServerUrl() {
    mcpConfig.getAzureDevOps().setServerUrl(null);
    assertThat(provider.isAvailable()).isFalse();
  }

  @Test
  void isAvailable_true_whenEnabledAndServerUrlSet() {
    assertThat(provider.isAvailable()).isTrue();
  }

  @Test
  void getProviderType_returnsAzureDevOps() {
    assertThat(provider.getProviderType()).isEqualTo("azure_devops");
  }

  // -----------------------------------------------------------------------
  // listFiles
  // -----------------------------------------------------------------------

  @Test
  void listFiles_notAvailable_returnsEmpty() {
    mcpConfig.getAzureDevOps().setEnabled(false);
    assertThat(provider.listFiles(context())).isEmpty();
    verifyNoInteractions(settingsService);
  }

  @Test
  void listFiles_parsesValueEnvelope_andFiltersFolders() throws Exception {
    String itemsJson = JSON.writeValueAsString(
        Map.of(
            "value",
            List.of(
                Map.of("path", "/src/Main.java", "isFolder", false),
                Map.of("path", "/src", "isFolder", true),
                Map.of("path", "/README.md", "isFolder", false))));
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse(itemsJson))
        .addHeader("Content-Type", "application/json"));

    List<String> files = provider.listFiles(context());

    assertThat(files).containsExactlyInAnyOrder("/src/Main.java", "/README.md");

    RecordedRequest recorded = server.takeRequest();
    assertThat(recorded.getPath()).endsWith("/mcp/v1/tools/call");
    assertThat(recorded.getHeader("Authorization")).startsWith("Basic ");

    @SuppressWarnings("unchecked")
    Map<String, Object> sentBody = JSON.readValue(recorded.getBody().readUtf8(), Map.class);
    assertThat(sentBody.get("method")).isEqualTo("tools/call");
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) sentBody.get("params");
    assertThat(params.get("name")).isEqualTo("azuredevops_get_items");
    @SuppressWarnings("unchecked")
    Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
    assertThat(arguments)
        .containsEntry("organization", "kixy")
        .containsEntry("project", "ShipFlow")
        .containsEntry("repositoryId", "shipflow-backend")
        .containsEntry("recursionLevel", "Full");
  }

  @Test
  void listFiles_parsesBareArrayOfPathStrings() throws Exception {
    String itemsJson = JSON.writeValueAsString(List.of("/a.txt", "/b.txt"));
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse(itemsJson)));

    assertThat(provider.listFiles(context())).containsExactlyInAnyOrder("/a.txt", "/b.txt");
  }

  @Test
  void listFiles_fallsBackToLineDelimitedParsing_whenResponseIsNotJson() throws Exception {
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse("src/a.txt\nsrc/b.txt\n")));

    assertThat(provider.listFiles(context())).containsExactly("src/a.txt", "src/b.txt");
  }

  @Test
  void listFiles_onServerError_returnsEmptyList() {
    server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));

    assertThat(provider.listFiles(context())).isEmpty();
  }

  @Test
  void listFiles_withBranch_includesBranchArgument() throws Exception {
    Map<String, String> ctx = context();
    ctx.put("branch", "release/1.0");
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse("[]")));

    provider.listFiles(ctx);

    RecordedRequest recorded = server.takeRequest();
    @SuppressWarnings("unchecked")
    Map<String, Object> sentBody = JSON.readValue(recorded.getBody().readUtf8(), Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) sentBody.get("params");
    @SuppressWarnings("unchecked")
    Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
    assertThat(arguments).containsEntry("branch", "release/1.0");
  }

  // -----------------------------------------------------------------------
  // readFile
  // -----------------------------------------------------------------------

  @Test
  void readFile_notAvailable_returnsEmpty() {
    mcpConfig.getAzureDevOps().setEnabled(false);
    assertThat(provider.readFile(context(), "README.md")).isEmpty();
  }

  @Test
  void readFile_returnsContent() throws Exception {
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse("public class Main {}")));

    Optional<String> content = provider.readFile(context(), "src/Main.java");

    assertThat(content).contains("public class Main {}");

    RecordedRequest recorded = server.takeRequest();
    @SuppressWarnings("unchecked")
    Map<String, Object> sentBody = JSON.readValue(recorded.getBody().readUtf8(), Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) sentBody.get("params");
    assertThat(params.get("name")).isEqualTo("azuredevops_get_item_content");
    @SuppressWarnings("unchecked")
    Map<String, Object> arguments = (Map<String, Object>) params.get("arguments");
    assertThat(arguments).containsEntry("path", "src/Main.java");
  }

  @Test
  void readFile_noResultInBody_returnsEmpty() throws Exception {
    server.enqueue(new MockResponse().setBody("{\"jsonrpc\":\"2.0\",\"id\":\"1\",\"error\":{\"message\":\"not found\"}}"));

    assertThat(provider.readFile(context(), "missing.txt")).isEmpty();
  }

  @Test
  void readFile_onServerError_returnsEmpty() {
    server.enqueue(new MockResponse().setResponseCode(500));

    assertThat(provider.readFile(context(), "src/Main.java")).isEmpty();
  }

  // -----------------------------------------------------------------------
  // searchFiles
  // -----------------------------------------------------------------------

  @Test
  void searchFiles_notAvailable_returnsEmpty() {
    mcpConfig.getAzureDevOps().setEnabled(false);
    assertThat(provider.searchFiles(context(), "TODO")).isEmpty();
  }

  @Test
  void searchFiles_usesSearchTool_whenMatchesFound() throws Exception {
    String matchesJson = JSON.writeValueAsString(
        List.of(Map.of("path", "/src/Todo.java", "isFolder", false)));
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse(matchesJson)));

    List<String> matches = provider.searchFiles(context(), "TODO");

    assertThat(matches).containsExactly("/src/Todo.java");

    RecordedRequest recorded = server.takeRequest();
    @SuppressWarnings("unchecked")
    Map<String, Object> sentBody = JSON.readValue(recorded.getBody().readUtf8(), Map.class);
    @SuppressWarnings("unchecked")
    Map<String, Object> params = (Map<String, Object>) sentBody.get("params");
    assertThat(params.get("name")).isEqualTo("azuredevops_search_code");
  }

  @Test
  void searchFiles_fallsBackToFilteredListing_whenSearchToolErrors() throws Exception {
    // First call (search_code) fails.
    server.enqueue(new MockResponse().setResponseCode(500));
    // Fallback call (listFiles) succeeds.
    String itemsJson = JSON.writeValueAsString(
        List.of(
            Map.of("path", "/src/Todo.java", "isFolder", false),
            Map.of("path", "/src/Other.java", "isFolder", false)));
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse(itemsJson)));

    List<String> matches = provider.searchFiles(context(), "Todo");

    assertThat(matches).containsExactly("/src/Todo.java");
    assertThat(server.getRequestCount()).isEqualTo(2);
  }

  @Test
  void searchFiles_fallsBackToFilteredListing_whenSearchToolReturnsNoMatches() throws Exception {
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse("[]")));
    String itemsJson = JSON.writeValueAsString(
        List.of(Map.of("path", "/src/Alpha.java", "isFolder", false)));
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse(itemsJson)));

    List<String> matches = provider.searchFiles(context(), "alpha");

    assertThat(matches).containsExactly("/src/Alpha.java");
  }

  // -----------------------------------------------------------------------
  // getResourceContext
  // -----------------------------------------------------------------------

  @Test
  void getResourceContext_notAvailable_returnsEmptyMap() {
    mcpConfig.getAzureDevOps().setEnabled(false);
    assertThat(provider.getResourceContext(context())).isEmpty();
  }

  @Test
  void getResourceContext_returnsResultMap() throws Exception {
    Map<String, Object> result = Map.of("provider", "azure_devops", "defaultBranch", "main");
    Map<String, Object> body = Map.of("jsonrpc", "2.0", "id", "1", "result", result);
    server.enqueue(new MockResponse().setBody(JSON.writeValueAsString(body)));

    Map<String, Object> resourceContext = provider.getResourceContext(context());

    assertThat(resourceContext).containsEntry("provider", "azure_devops");
  }

  @Test
  void getResourceContext_onServerError_returnsEmptyMap() {
    server.enqueue(new MockResponse().setResponseCode(500));

    assertThat(provider.getResourceContext(context())).isEmpty();
  }

  // -----------------------------------------------------------------------
  // Auth
  // -----------------------------------------------------------------------

  @Test
  void requestsProceedWithoutAuthHeader_whenNoTokenConfigured() throws Exception {
    when(settingsService.getAzureDevOpsAccessToken()).thenReturn(null);
    server.enqueue(new MockResponse().setBody(jsonRpcResultResponse("[]")));

    provider.listFiles(context());

    RecordedRequest recorded = server.takeRequest();
    assertThat(recorded.getHeader("Authorization")).isNull();
  }
}
