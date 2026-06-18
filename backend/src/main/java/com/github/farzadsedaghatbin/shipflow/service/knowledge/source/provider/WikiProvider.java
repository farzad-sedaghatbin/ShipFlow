package com.github.farzadsedaghatbin.shipflow.service.knowledge.source.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpacePermissionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.*;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WikiProvider implements KnowledgeSourceProvider {

  private static final int CHUNK_SIZE = 1200;
  private static final int CHUNK_OVERLAP = 150;
  private static final ObjectMapper JSON = new ObjectMapper();

  private final WikiSpaceRepository wikiSpaceRepository;
  private final WikiPageRepository wikiPageRepository;
  private final WikiSpacePermissionRepository wikiSpacePermissionRepository;

  @Override
  public KnowledgeProviderType getType() {
    return KnowledgeProviderType.WIKI;
  }

  @Override
  public boolean supportsRefresh() {
    return true;
  }

  @Override
  public void validateConfig(JsonNode config) {
    if (!config.hasNonNull("spaceId")) {
      throw new InvalidConfigException("spaceId is required");
    }
    long spaceId = config.get("spaceId").asLong();
    if (wikiSpaceRepository.findById(spaceId).filter(s -> s.getDeletedAt() == null).isEmpty()) {
      throw new InvalidConfigException("Wiki space not found: " + spaceId);
    }
  }

  @Override
  public IngestionResult ingest(KnowledgeSource source, IngestionContext ctx) {
    try {
      JsonNode cfg = JSON.readTree(source.getConfig());
      long spaceId = cfg.get("spaceId").asLong();

      WikiSpace space =
          wikiSpaceRepository
              .findById(spaceId)
              .orElseThrow(() -> new InvalidConfigException("Wiki space not found: " + spaceId));

      // Heuristic mitigation: restricted spaces (explicit per-space grants) are excluded from KC
      // ingestion pending true ACL-aware retrieval support.
      if (wikiSpacePermissionRepository.existsBySpaceId(spaceId)) {
        return IngestionResult.builder()
            .chunks(java.util.List.of())
            .sourceMetadata(java.util.Map.of("spaceId", spaceId, "skipped", "restricted"))
            .build();
      }

      List<WikiPage> pages = wikiPageRepository.findBySpaceIdAndDeletedAtIsNull(spaceId);
      List<RawChunk> chunks = new ArrayList<>();

      for (WikiPage page : pages) {
        String text = page.getContentText();
        if (text == null || text.isBlank()) continue;

        String sourceUrl = "/wiki/" + space.getSpaceKey() + "/" + page.getId();
        int ord = 0;
        for (int i = 0; i < text.length(); i += CHUNK_SIZE - CHUNK_OVERLAP) {
          String body = text.substring(i, Math.min(i + CHUNK_SIZE, text.length()));
          chunks.add(
              RawChunk.builder()
                  .title(page.getTitle() + " — part " + (ord + 1))
                  .content(body)
                  .ordinal(ord++)
                  .sourceUrl(sourceUrl)
                  .hash(sha256Hex(body))
                  .build());
          if (i + CHUNK_SIZE >= text.length()) break;
        }
      }

      return IngestionResult.builder()
          .chunks(chunks)
          .sourceMetadata(java.util.Map.of("spaceId", spaceId))
          .build();
    } catch (RuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException("Wiki ingest failed: " + e.getMessage(), e);
    }
  }

  private static String sha256Hex(String s) {
    try {
      var d = MessageDigest.getInstance("SHA-256").digest(s.getBytes());
      var sb = new StringBuilder();
      for (byte x : d) sb.append(String.format("%02x", x));
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
