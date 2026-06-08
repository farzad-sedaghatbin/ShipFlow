package com.github.farzadsedaghatbin.shipflow.service.inbound;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.inbound.InboundWebhookConfig;
import com.github.farzadsedaghatbin.shipflow.repository.inbound.InboundWebhookConfigRepository;
import com.github.farzadsedaghatbin.shipflow.service.github.GitHubIntegrationService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Inbound webhook handler for GitHub events received at {@code /api/inbound/github}.
 *
 * <p>Handles {@code pull_request} events: when a PR is closed <em>and merged</em>, scans the
 * PR title and body for {@code closes #N} / {@code fixes #N} / {@code resolves #N} keywords and
 * marks the referenced ShipFlow tasks as {@code DONE}.</p>
 *
 * <p>Signature validation uses HMAC-SHA256 against the secret stored in the
 * {@code inbound_webhook_config} row for provider {@code "github"}.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GitHubInboundWebhookHandler implements InboundWebhookHandler {

  private static final String PROVIDER = "github";
  private static final String SIG_HEADER = "x-hub-signature-256";

  // Matches task-195, sf-195, KIXY-195, SHIP-195, kixy-195 etc. anywhere in a branch name.
  // The [A-Z]{2,10} arm covers project-code style (Jira-like) conventions.
  private static final Pattern BRANCH_TASK_PATTERN =
      Pattern.compile("(?:task|sf|[a-zA-Z]{2,10})[_-](\\d+)", Pattern.CASE_INSENSITIVE);

  private final InboundWebhookConfigRepository configRepository;
  private final GitHubIntegrationService gitHubIntegrationService;
  private final ObjectMapper objectMapper;

  @Override
  public String getProviderName() {
    return PROVIDER;
  }

  @Override
  public boolean isActive() {
    return configRepository.findByProviderName(PROVIDER)
        .map(InboundWebhookConfig::getIsEnabled)
        .orElse(false);
  }

  @Override
  public boolean validateSignature(String payload, Map<String, String> headers) {
    InboundWebhookConfig config = configRepository.findByProviderName(PROVIDER).orElse(null);
    if (config == null) return false;

    String secret = config.getWebhookSecret();
    if (secret == null || secret.isBlank()) {
      log.debug("No secret configured for github inbound webhook — skipping signature check");
      return true;
    }

    String providedSig = headers.get(SIG_HEADER);
    if (providedSig == null || providedSig.isBlank()) {
      log.warn("Missing {} header in GitHub inbound webhook request", SIG_HEADER);
      return false;
    }

    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
      byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
      String computed = bytesToHex(hash);
      String stripped = providedSig.startsWith("sha256=") ? providedSig.substring(7) : providedSig;
      return computed.equalsIgnoreCase(stripped.trim());
    } catch (Exception e) {
      log.error("GitHub inbound webhook signature validation error: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public Map<String, Object> handle(String eventType, String payload, Map<String, String> headers) {
    if ("pull_request".equals(eventType)) {
      return handlePullRequest(payload);
    }
    log.debug("GitHub inbound webhook: ignoring event type '{}'", eventType);
    return Map.of("status", "accepted", "eventType", eventType);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> handlePullRequest(String payload) {
    try {
      Map<String, Object> body = objectMapper.readValue(payload,
          new TypeReference<Map<String, Object>>() {});
      String action = (String) body.get("action");

      if (!"closed".equals(action)) {
        return Map.of("status", "skipped", "reason", "action=" + action);
      }

      Map<String, Object> pr = (Map<String, Object>) body.get("pull_request");
      if (pr == null) {
        log.warn("GitHub pull_request webhook missing pull_request field");
        return Map.of("status", "error", "reason", "missing pull_request payload");
      }

      Boolean merged = (Boolean) pr.get("merged");
      if (!Boolean.TRUE.equals(merged)) {
        return Map.of("status", "skipped", "reason", "PR closed but not merged");
      }

      String title = (String) pr.get("title");
      String description = (String) pr.get("body");
      Integer prNumber = (Integer) pr.get("number");
      String branch = (String) ((Map<String, Object>) pr.get("head")).get("ref");
      String mergedAtStr = (String) pr.get("merged_at");
      LocalDateTime mergedAt = mergedAtStr != null
          ? ZonedDateTime.parse(mergedAtStr).toLocalDateTime()
          : LocalDateTime.now();

      log.info("GitHub inbound webhook: processing merged PR #{} (branch: {}) — scanning for task references",
          prNumber, branch);
      gitHubIntegrationService.autoCloseTasksFromMergedPR(title, description, prNumber, mergedAt);
      closeTasksFromBranchName(branch, prNumber, mergedAt);

      return Map.of("status", "accepted", "action", "auto_close_attempted", "pr", prNumber);
    } catch (Exception e) {
      log.error("Error handling GitHub pull_request inbound event: {}", e.getMessage(), e);
      Map<String, Object> err = new HashMap<>();
      err.put("status", "error");
      err.put("message", e.getMessage());
      return err;
    }
  }

  private void closeTasksFromBranchName(String branch, Integer prNumber, LocalDateTime mergedAt) {
    if (branch == null || branch.isBlank()) return;
    Matcher m = BRANCH_TASK_PATTERN.matcher(branch);
    while (m.find()) {
      try {
        Long taskId = Long.parseLong(m.group(1));
        gitHubIntegrationService.closeTaskByIdFromMergedPR(taskId, prNumber, mergedAt);
        log.info("GitHub inbound webhook: auto-close from branch '{}' matched task #{}", branch, taskId);
      } catch (NumberFormatException ignored) {
      }
    }
  }

  private static String bytesToHex(byte[] bytes) {
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }
}
