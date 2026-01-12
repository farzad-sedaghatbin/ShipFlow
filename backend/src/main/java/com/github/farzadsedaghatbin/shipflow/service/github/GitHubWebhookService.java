package com.github.farzadsedaghatbin.shipflow.service.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.github.GitHubWebhookEvent;
import com.github.farzadsedaghatbin.shipflow.repository.github.GitHubWebhookEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.Formatter;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class GitHubWebhookService {

    private final GitHubWebhookEventRepository webhookEventRepository;
    private final GitHubIntegrationService integrationService;
    private final ObjectMapper objectMapper;

    private static final String HMAC_SHA256 = "HmacSHA256";

    /**
     * Validate GitHub webhook signature
     */
    public boolean validateSignature(String payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            return false;
        }

        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            mac.init(secretKeySpec);
            
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = "sha256=" + byteArrayToHex(hash);
            
            return expectedSignature.equals(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Error validating webhook signature", e);
            return false;
        }
    }

    /**
     * Process a webhook event
     */
    public void processWebhook(String eventType, String payload) {
        // Store the event
        GitHubWebhookEvent event = GitHubWebhookEvent.builder()
                .eventType(eventType)
                .payload(payload)
                .processed(false)
                .build();

        try {
            JsonNode root = objectMapper.readTree(payload);
            
            // Extract repository information
            JsonNode repoNode = root.get("repository");
            if (repoNode != null) {
                String repoFullName = repoNode.get("full_name").asText();
                event.setRepositoryFullName(repoFullName);
            }

            event = webhookEventRepository.save(event);

            // Process based on event type
            switch (eventType) {
                case "push":
                    processPushEvent(root, event);
                    break;
                case "pull_request":
                    processPullRequestEvent(root, event);
                    break;
                case "create":
                case "delete":
                    processBranchEvent(root, event, eventType);
                    break;
                default:
                    log.info("Unhandled webhook event type: {}", eventType);
            }

            event.setProcessed(true);
            event.setProcessedAt(LocalDateTime.now());
            webhookEventRepository.save(event);

        } catch (Exception e) {
            log.error("Error processing webhook event", e);
            event.setProcessed(false);
            event.setErrorMessage(e.getMessage());
            webhookEventRepository.save(event);
        }
    }

    /**
     * Process push event (new commits)
     */
    private void processPushEvent(JsonNode root, GitHubWebhookEvent event) {
        try {
            JsonNode repoNode = root.get("repository");
            String repoFullName = repoNode.get("full_name").asText();
            String ref = root.get("ref").asText(); // refs/heads/main
            String branch = ref.replace("refs/heads/", "");
            
            JsonNode commitsNode = root.get("commits");
            if (commitsNode != null && commitsNode.isArray()) {
                for (JsonNode commitNode : commitsNode) {
                    String sha = commitNode.get("id").asText();
                    String message = commitNode.get("message").asText();
                    String timestamp = commitNode.get("timestamp").asText();
                    String url = commitNode.get("url").asText();
                    
                    JsonNode authorNode = commitNode.get("author");
                    String authorName = authorNode.get("name").asText();
                    String authorEmail = authorNode.get("email").asText();
                    String authorUsername = authorNode.has("username") ? authorNode.get("username").asText() : null;
                    
                    LocalDateTime commitDate = ZonedDateTime.parse(timestamp).toLocalDateTime();
                    
                    integrationService.processCommit(
                        repoFullName, sha, message, authorName, authorEmail, 
                        authorUsername, commitDate, branch, url
                    );
                    
                    log.info("Processed commit {} from push event", sha);
                }
            }
        } catch (Exception e) {
            log.error("Error processing push event", e);
            throw new RuntimeException("Failed to process push event", e);
        }
    }

    /**
     * Process pull request event
     */
    private void processPullRequestEvent(JsonNode root, GitHubWebhookEvent event) {
        try {
            String action = root.get("action").asText();
            JsonNode prNode = root.get("pull_request");
            JsonNode repoNode = root.get("repository");
            
            String repoFullName = repoNode.get("full_name").asText();
            Integer prNumber = prNode.get("number").asInt();
            String title = prNode.get("title").asText();
            String description = prNode.has("body") && !prNode.get("body").isNull() 
                    ? prNode.get("body").asText() : null;
            String state = prNode.get("state").asText(); // open or closed
            
            JsonNode headNode = prNode.get("head");
            JsonNode baseNode = prNode.get("base");
            String headBranch = headNode.get("ref").asText();
            String baseBranch = baseNode.get("ref").asText();
            
            JsonNode userNode = prNode.get("user");
            String authorUsername = userNode.get("login").asText();
            String url = prNode.get("html_url").asText();
            
            String createdAt = prNode.get("created_at").asText();
            LocalDateTime openedAt = ZonedDateTime.parse(createdAt).toLocalDateTime();
            
            LocalDateTime closedAt = null;
            if (prNode.has("closed_at") && !prNode.get("closed_at").isNull()) {
                closedAt = ZonedDateTime.parse(prNode.get("closed_at").asText()).toLocalDateTime();
            }
            
            LocalDateTime mergedAt = null;
            String mergedByUsername = null;
            if (prNode.has("merged_at") && !prNode.get("merged_at").isNull()) {
                mergedAt = ZonedDateTime.parse(prNode.get("merged_at").asText()).toLocalDateTime();
                state = "MERGED";
            }
            if (prNode.has("merged_by") && !prNode.get("merged_by").isNull()) {
                mergedByUsername = prNode.get("merged_by").get("login").asText();
            }
            
            integrationService.processPullRequest(
                repoFullName, prNumber, title, description, state,
                headBranch, baseBranch, authorUsername, url,
                openedAt, closedAt, mergedAt, mergedByUsername
            );
            
            log.info("Processed PR #{} ({}) from webhook", prNumber, action);
        } catch (Exception e) {
            log.error("Error processing pull request event", e);
            throw new RuntimeException("Failed to process pull request event", e);
        }
    }

    /**
     * Process branch creation/deletion event
     */
    private void processBranchEvent(JsonNode root, GitHubWebhookEvent event, String eventType) {
        try {
            String refType = root.get("ref_type").asText();
            if (!"branch".equals(refType)) {
                return; // Only process branch events
            }
            
            String ref = root.get("ref").asText(); // branch name
            log.info("Processed branch {} event for branch: {}", eventType, ref);
            
            // Branch creation/deletion handling can be implemented here if needed
        } catch (Exception e) {
            log.error("Error processing branch event", e);
            throw new RuntimeException("Failed to process branch event", e);
        }
    }

    private String byteArrayToHex(byte[] bytes) {
        try (Formatter formatter = new Formatter()) {
            for (byte b : bytes) {
                formatter.format("%02x", b);
            }
            return formatter.toString();
        }
    }
}
