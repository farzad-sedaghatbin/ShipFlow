package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Async orchestrator that listens for {@link KnowledgeSourceCreatedEvent} and runs the configured
 * provider to fetch + chunk source content, then persists chunks via {@link
 * KnowledgeIngestionService} and flips the source status to {@code READY} or {@code FAILED}.
 *
 * <p>Runs on the shared async executor so HTTP request threads return immediately after the source
 * row is created. Retries the entire ingest once after a 30s backoff on any transient exception.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IngestionOrchestrator {

  private final KnowledgeSourceRepository sources;
  private final KnowledgeSourceRegistry registry;
  private final KnowledgeIngestionService ingest;
  private final KnowledgeSourceService svc;
  private final ObjectMapper json;

  @Async
  @EventListener
  @Retryable(maxAttempts = 2, backoff = @Backoff(delay = 30_000))
  public void onCreated(KnowledgeSourceCreatedEvent event) {
    var src = sources.findActiveById(event.sourceId()).orElse(null);
    if (src == null) {
      log.warn("Ingest skipped, source {} not found", event.sourceId());
      return;
    }

    svc.markIngesting(src.getId());
    try {
      var provider = registry.get(src.getProviderType());
      var ctx = IngestionContext.builder().currentUserId(src.getCreatedBy()).build();
      var result = provider.ingest(src, ctx);

      ingest.ingestChunks(
          result.getChunks(),
          KnowledgeEntityType.KNOWLEDGE_SOURCE,
          src.getId(),
          src.getTeamId(),
          src.getProjectId());

      JsonNode existingCfg = json.readTree(src.getConfig());
      ObjectNode merged =
          (existingCfg instanceof ObjectNode on) ? on.deepCopy() : json.createObjectNode();
      if (result.getSourceMetadata() != null) {
        result.getSourceMetadata().forEach(merged::putPOJO);
      }
      svc.markReady(src.getId(), merged);

    } catch (Exception e) {
      log.error("Knowledge source ingest failed for source {}", src.getId(), e);
      svc.markFailed(src.getId(), e.getMessage());
    }
  }
}
