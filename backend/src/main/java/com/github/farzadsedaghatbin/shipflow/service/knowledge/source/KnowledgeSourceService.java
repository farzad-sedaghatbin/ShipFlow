package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.ChunkPreview;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.CreateKnowledgeSourceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.KnowledgeSourceResponse;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.exception.ResourceNotFoundException;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceDeletedEvent;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceUpdatedEvent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CRUD orchestration for knowledge sources.
 *
 * <p>Persistence + ACL gating + event fan-out. Actual chunk fetching happens in the orchestrator
 * (Task 7) which listens for {@link KnowledgeSourceCreatedEvent}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeSourceService {

  private static final int MAX_ERROR_LENGTH = 1000;

  private final KnowledgeSourceRepository sources;
  private final KnowledgeItemRepository items;
  private final KnowledgeSourceRegistry registry;
  private final KnowledgeSourceAccessChecker acl;
  private final EmbeddingStore<TextSegment> embeddingStore;
  private final ApplicationEventPublisher publisher;
  private final ObjectMapper json;
  private final KnowledgeIngestionService ingestionService;

  @Transactional
  public KnowledgeSourceResponse create(CreateKnowledgeSourceRequest req, Long currentUserId) {
    acl.assertCanCreate(req.getScope(), req.getTeamId(), req.getProjectId(), currentUserId);

    var provider = registry.get(req.getProviderType());
    try {
      provider.validateConfig(req.getConfig());
    } catch (InvalidConfigException e) {
      throw new IllegalArgumentException("Invalid provider config: " + e.getMessage(), e);
    }

    KnowledgeSource entity =
        KnowledgeSource.builder()
            .name(req.getName())
            .description(req.getDescription())
            .providerType(req.getProviderType())
            .scope(req.getScope())
            .teamId(req.getTeamId())
            .projectId(req.getProjectId())
            .config(writeJson(req.getConfig()))
            .status(KnowledgeSourceStatus.PENDING)
            .createdBy(currentUserId)
            .build();

    KnowledgeSource saved = sources.save(entity);
    publisher.publishEvent(new KnowledgeSourceCreatedEvent(saved.getId()));
    return toResponse(saved);
  }

  /**
   * Variant of {@link #create} used by the multipart upload endpoint.
   *
   * <p>Runs the ingest <em>synchronously</em> because the {@link InputStream} from the multipart
   * request cannot survive past the controller thread. Status transitions {@code INGESTING} →
   * {@code READY} / {@code FAILED} happen inline rather than via the async orchestrator.
   */
  @Transactional
  public KnowledgeSourceResponse createWithUpload(
      CreateKnowledgeSourceRequest req,
      Long currentUserId,
      InputStream stream,
      String filename,
      String contentType) {

    acl.assertCanCreate(req.getScope(), req.getTeamId(), req.getProjectId(), currentUserId);
    var provider = registry.get(req.getProviderType());
    try {
      provider.validateConfig(req.getConfig());
    } catch (InvalidConfigException e) {
      throw new IllegalArgumentException("Invalid provider config: " + e.getMessage(), e);
    }

    KnowledgeSource entity =
        KnowledgeSource.builder()
            .name(req.getName())
            .description(req.getDescription())
            .providerType(req.getProviderType())
            .scope(req.getScope())
            .teamId(req.getTeamId())
            .projectId(req.getProjectId())
            .config(writeJson(req.getConfig()))
            .status(KnowledgeSourceStatus.INGESTING)
            .createdBy(currentUserId)
            .build();
    KnowledgeSource saved = sources.save(entity);

    try {
      IngestionContext ctx =
          IngestionContext.builder()
              .currentUserId(currentUserId)
              .uploadStream(stream)
              .uploadOriginalFilename(filename)
              .uploadContentType(contentType)
              .build();
      IngestionResult result = provider.ingest(saved, ctx);

      ingestionService.ingestChunks(
          result.getChunks(),
          KnowledgeEntityType.KNOWLEDGE_SOURCE,
          saved.getId(),
          saved.getTeamId(),
          saved.getProjectId());

      JsonNode existingCfg = json.readTree(saved.getConfig());
      ObjectNode merged =
          (existingCfg instanceof ObjectNode on) ? on.deepCopy() : json.createObjectNode();
      if (result.getSourceMetadata() != null) {
        result.getSourceMetadata().forEach(merged::putPOJO);
      }
      saved.setConfig(merged.toString());
      saved.setStatus(KnowledgeSourceStatus.READY);
      saved.setLastIngestedAt(OffsetDateTime.now());
      saved.setLastError(null);
      saved = sources.save(saved);

      publisher.publishEvent(new KnowledgeSourceUpdatedEvent(saved.getId()));
      return toResponse(saved);
    } catch (Exception e) {
      log.error("Upload ingestion failed for source {}", saved.getId(), e);
      saved.setStatus(KnowledgeSourceStatus.FAILED);
      saved.setLastError(truncateError(e.getMessage()));
      sources.save(saved);
      publisher.publishEvent(new KnowledgeSourceUpdatedEvent(saved.getId()));
      throw new RuntimeException("Upload ingestion failed: " + e.getMessage(), e);
    }
  }

  @Transactional(readOnly = true)
  public List<KnowledgeSourceResponse> listOrg(Long currentUserId) {
    acl.assertCanListOrg(currentUserId);
    return sources.findActiveByOrgScope().stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<KnowledgeSourceResponse> listTeam(Long teamId, Long currentUserId) {
    acl.assertCanListTeam(teamId, currentUserId);
    return sources.findActiveByTeamScope(teamId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<KnowledgeSourceResponse> listProject(Long projectId, Long currentUserId) {
    acl.assertCanListProject(projectId, currentUserId);
    return sources.findActiveByProjectScope(projectId).stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public List<ChunkPreview> previewChunks(Long sourceId, Long currentUserId) {
    KnowledgeSource src = loadActive(sourceId);
    acl.assertCanModify(src, currentUserId);
    return items.findByKnowledgeSourceIdOrderByChunkIndexAsc(sourceId).stream()
        .limit(10)
        .map(
            item ->
                ChunkPreview.builder()
                    .id(item.getId())
                    .title(item.getTitle())
                    .contentPreview(
                        item.getContent() == null
                            ? ""
                            : item.getContent()
                                .substring(0, Math.min(400, item.getContent().length())))
                    .ordinal(item.getChunkIndex() == null ? 0 : item.getChunkIndex())
                    .embedded(Boolean.TRUE.equals(item.getIsEmbedded()))
                    .build())
        .toList();
  }

  @Transactional
  public void requestRefresh(Long sourceId, Long currentUserId) {
    KnowledgeSource src = loadActive(sourceId);
    acl.assertCanModify(src, currentUserId);
    src.setStatus(KnowledgeSourceStatus.PENDING);
    src.setLastError(null);
    sources.save(src);
    // Reuse the orchestrator path; it will pull, ingest, and flip to READY/FAILED.
    publisher.publishEvent(new KnowledgeSourceCreatedEvent(src.getId()));
  }

  @Transactional
  public void delete(Long sourceId, Long currentUserId) {
    KnowledgeSource src = loadActive(sourceId);
    acl.assertCanModify(src, currentUserId);

    List<Long> itemIds = items.findIdsBySourceId(sourceId);
    OffsetDateTime now = OffsetDateTime.now();

    if (!itemIds.isEmpty()) {
      items.softDeleteBySourceId(sourceId, now);
      List<String> idsAsStrings = itemIds.stream().map(String::valueOf).toList();
      try {
        embeddingStore.removeAll(idsAsStrings);
      } catch (RuntimeException e) {
        // Vector store removal is best-effort: the row is still soft-deleted, so retrieval will
        // skip it. Log and continue so the operation remains idempotent.
        log.warn(
            "Failed to remove {} embeddings for deleted KnowledgeSource {}: {}",
            idsAsStrings.size(),
            sourceId,
            e.getMessage());
      }
    }

    src.setDeletedAt(now);
    sources.save(src);

    publisher.publishEvent(new KnowledgeSourceDeletedEvent(sourceId, itemIds));
  }

  // ---------- package-private status helpers (Tasks 7 + 10) ----------

  @Transactional
  void markIngesting(Long id) {
    KnowledgeSource src = loadActive(id);
    src.setStatus(KnowledgeSourceStatus.INGESTING);
    sources.save(src);
    publisher.publishEvent(new KnowledgeSourceUpdatedEvent(id));
  }

  @Transactional
  void markReady(Long id, JsonNode mergedConfig) {
    KnowledgeSource src = loadActive(id);
    src.setStatus(KnowledgeSourceStatus.READY);
    src.setLastIngestedAt(OffsetDateTime.now());
    src.setLastError(null);
    if (mergedConfig != null) {
      src.setConfig(writeJson(mergedConfig));
    }
    sources.save(src);
    publisher.publishEvent(new KnowledgeSourceUpdatedEvent(id));
  }

  @Transactional
  void markFailed(Long id, String err) {
    KnowledgeSource src = loadActive(id);
    src.setStatus(KnowledgeSourceStatus.FAILED);
    src.setLastError(truncateError(err));
    sources.save(src);
    publisher.publishEvent(new KnowledgeSourceUpdatedEvent(id));
  }

  @Transactional
  void markStale(Long id) {
    KnowledgeSource src = loadActive(id);
    src.setStatus(KnowledgeSourceStatus.STALE);
    sources.save(src);
    publisher.publishEvent(new KnowledgeSourceUpdatedEvent(id));
  }

  // ---------- helpers ----------

  private KnowledgeSource loadActive(Long id) {
    return sources
        .findActiveById(id)
        .orElseThrow(() -> new ResourceNotFoundException("KnowledgeSource " + id + " not found"));
  }

  private KnowledgeSourceResponse toResponse(KnowledgeSource src) {
    long chunkCount = src.getId() == null ? 0L : items.countActiveBySourceId(src.getId());
    return KnowledgeSourceResponse.from(src, chunkCount);
  }

  private String writeJson(JsonNode node) {
    try {
      return json.writeValueAsString(node);
    } catch (JsonProcessingException e) {
      throw new IllegalArgumentException("Unable to serialize provider config", e);
    }
  }

  private static String truncateError(String err) {
    if (err == null) return null;
    return err.length() <= MAX_ERROR_LENGTH ? err : err.substring(0, MAX_ERROR_LENGTH);
  }
}
