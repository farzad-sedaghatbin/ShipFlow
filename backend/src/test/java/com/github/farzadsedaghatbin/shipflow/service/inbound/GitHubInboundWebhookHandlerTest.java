package com.github.farzadsedaghatbin.shipflow.service.inbound;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.inbound.InboundWebhookConfig;
import com.github.farzadsedaghatbin.shipflow.repository.inbound.InboundWebhookConfigRepository;
import com.github.farzadsedaghatbin.shipflow.service.github.GitHubIntegrationService;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GitHubInboundWebhookHandlerTest {

  @Mock
  private InboundWebhookConfigRepository configRepository;
  @Mock
  private GitHubIntegrationService gitHubIntegrationService;

  private GitHubInboundWebhookHandler handler;
  private final ObjectMapper objectMapper = new ObjectMapper();

  @BeforeEach
  void setUp() {
    handler = new GitHubInboundWebhookHandler(configRepository, gitHubIntegrationService,
        objectMapper);
  }

  @Test
  void getProviderName_returnsGithub() {
    assertThat(handler.getProviderName()).isEqualTo("github");
  }

  @Test
  void isActive_trueWhenConfigEnabled() {
    InboundWebhookConfig config = new InboundWebhookConfig();
    config.setIsEnabled(true);
    when(configRepository.findByProviderName("github")).thenReturn(Optional.of(config));
    assertThat(handler.isActive()).isTrue();
  }

  @Test
  void isActive_falseWhenNoConfig() {
    when(configRepository.findByProviderName("github")).thenReturn(Optional.empty());
    assertThat(handler.isActive()).isFalse();
  }

  @Test
  void isActive_falseWhenConfigDisabled() {
    InboundWebhookConfig config = new InboundWebhookConfig();
    config.setIsEnabled(false);
    when(configRepository.findByProviderName("github")).thenReturn(Optional.of(config));
    assertThat(handler.isActive()).isFalse();
  }

  @Test
  void validateSignature_trueForValidHmacSha256() throws Exception {
    String secret = "test-secret";
    String payload = "{\"action\":\"closed\"}";

    Mac mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
    StringBuilder hex = new StringBuilder();
    for (byte b : hash) {
      hex.append(String.format("%02x", b));
    }
    String sig = "sha256=" + hex;

    InboundWebhookConfig config = new InboundWebhookConfig();
    config.setWebhookSecret(secret);
    when(configRepository.findByProviderName("github")).thenReturn(Optional.of(config));

    assertThat(handler.validateSignature(payload, Map.of("x-hub-signature-256", sig))).isTrue();
  }

  @Test
  void validateSignature_falseForWrongSignature() {
    InboundWebhookConfig config = new InboundWebhookConfig();
    config.setWebhookSecret("real-secret");
    when(configRepository.findByProviderName("github")).thenReturn(Optional.of(config));

    assertThat(handler.validateSignature("{}", Map.of("x-hub-signature-256", "sha256=badhex")))
        .isFalse();
  }

  @Test
  void validateSignature_falseWhenMissingHeader() {
    InboundWebhookConfig config = new InboundWebhookConfig();
    config.setWebhookSecret("secret");
    when(configRepository.findByProviderName("github")).thenReturn(Optional.of(config));

    assertThat(handler.validateSignature("{}", Map.of())).isFalse();
  }

  @Test
  void validateSignature_trueWhenNoSecretConfigured() {
    InboundWebhookConfig config = new InboundWebhookConfig();
    config.setWebhookSecret(null);
    when(configRepository.findByProviderName("github")).thenReturn(Optional.of(config));

    assertThat(handler.validateSignature("{}", Map.of())).isTrue();
  }

  @Test
  void handle_closedMergedPR_callsAutoClose() {
    String payload = """
        {
          "action": "closed",
          "pull_request": {
            "number": 42,
            "title": "closes #195",
            "body": "fixes the bug",
            "merged": true,
            "merged_at": "2026-06-04T12:00:00Z",
            "head": {"ref": "feat/some-branch"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("accepted");
    verify(gitHubIntegrationService).autoCloseTasksFromMergedPR(
        eq("closes #195"), eq("fixes the bug"), eq(42), any());
  }

  @Test
  void handle_branchNameTaskId_closesTaskDirectly() {
    String payload = """
        {
          "action": "closed",
          "pull_request": {
            "number": 10,
            "title": "my pr",
            "body": null,
            "merged": true,
            "merged_at": "2026-06-04T12:00:00Z",
            "head": {"ref": "feat/task-195-fix-login"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("accepted");
    verify(gitHubIntegrationService).closeTaskByIdFromMergedPR(eq(195L), eq(10), any());
  }

  @Test
  void handle_branchNameWithPrefixVariants_closesTask() {
    String payload = """
        {
          "action": "closed",
          "pull_request": {
            "number": 5,
            "title": "my pr",
            "body": null,
            "merged": true,
            "merged_at": "2026-06-04T12:00:00Z",
            "head": {"ref": "testt-task-195"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("accepted");
    verify(gitHubIntegrationService).closeTaskByIdFromMergedPR(eq(195L), eq(5), any());
  }

  @Test
  void handle_sfPrefixBranch_closesTask() {
    String payload = """
        {
          "action": "closed",
          "pull_request": {
            "number": 3,
            "title": "my pr",
            "body": null,
            "merged": true,
            "merged_at": "2026-06-04T12:00:00Z",
            "head": {"ref": "feat/SF-42-add-feature"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("accepted");
    verify(gitHubIntegrationService).closeTaskByIdFromMergedPR(eq(42L), eq(3), any());
  }

  @Test
  void handle_projectCodeBranch_closesTask() {
    String payload = """
        {
          "action": "closed",
          "pull_request": {
            "number": 8,
            "title": "my pr",
            "body": null,
            "merged": true,
            "merged_at": "2026-06-04T12:00:00Z",
            "head": {"ref": "feat/KIXY-195-fix-login"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("accepted");
    verify(gitHubIntegrationService).closeTaskByIdFromMergedPR(eq(195L), eq(8), any());
  }

  @Test
  void handle_closedNotMergedPR_skips() {
    String payload = """
        {
          "action": "closed",
          "pull_request": {
            "number": 42,
            "title": "some pr",
            "body": null,
            "merged": false,
            "merged_at": null,
            "head": {"ref": "feat/task-200"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("skipped");
    verify(gitHubIntegrationService, never())
        .autoCloseTasksFromMergedPR(any(), any(), any(), any());
    verify(gitHubIntegrationService, never())
        .closeTaskByIdFromMergedPR(any(), any(), any());
  }

  @Test
  void handle_openedPR_skips() {
    String payload = """
        {
          "action": "opened",
          "pull_request": {
            "number": 42,
            "title": "some pr",
            "body": null,
            "merged": false,
            "merged_at": null,
            "head": {"ref": "feat/task-200"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("skipped");
    verify(gitHubIntegrationService, never())
        .autoCloseTasksFromMergedPR(any(), any(), any(), any());
  }

  @Test
  void handle_pushEvent_accepted() {
    Map<String, Object> result = handler.handle("push", "{}", Map.of());

    assertThat(result.get("status")).isEqualTo("accepted");
    verify(gitHubIntegrationService, never())
        .autoCloseTasksFromMergedPR(any(), any(), any(), any());
  }

  @Test
  void handle_nullBody_autoCloseCalledWithNull() {
    String payload = """
        {
          "action": "closed",
          "pull_request": {
            "number": 7,
            "title": "fixes #12",
            "body": null,
            "merged": true,
            "merged_at": "2026-06-04T10:00:00Z",
            "head": {"ref": "feat/some-branch"}
          }
        }
        """;

    Map<String, Object> result = handler.handle("pull_request", payload, Map.of());

    assertThat(result.get("status")).isEqualTo("accepted");
    verify(gitHubIntegrationService).autoCloseTasksFromMergedPR(
        eq("fixes #12"), isNull(), eq(7), any());
  }
}
