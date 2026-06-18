package com.github.farzadsedaghatbin.shipflow.service.knowledge;

import com.github.farzadsedaghatbin.shipflow.entity.WikiPage;
import com.github.farzadsedaghatbin.shipflow.entity.WikiSpace;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.event.WikiPageChangedEvent;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiPageRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WikiSpaceRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.RawChunk;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WikiKnowledgeListener {

  private static final int CHUNK_SIZE = 1200;
  private static final int CHUNK_OVERLAP = 150;

  private final WikiPageRepository wikiPageRepository;
  private final WikiSpaceRepository wikiSpaceRepository;
  private final KnowledgeItemRepository knowledgeItemRepository;
  private final KnowledgeIngestionService knowledgeIngestionService;

  @Async
  @EventListener
  public void onWikiPageChanged(WikiPageChangedEvent event) {
    try {
      Long pageId = event.pageId();
      Long spaceId = event.spaceId();

      switch (event.type()) {
        case CREATED, UPDATED, RESTORED -> handleUpsert(pageId, spaceId);
        case DELETED -> handleDelete(pageId);
      }
    } catch (Exception e) {
      log.error(
          "WikiKnowledgeListener failed for event {}: {}", event, e.getMessage(), e);
    }
  }

  private void handleUpsert(Long pageId, Long spaceId) {
    // Remove existing knowledge items for this page before re-ingesting
    knowledgeItemRepository.deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);

    WikiPage page = wikiPageRepository.findById(pageId).orElse(null);
    if (page == null || page.getDeletedAt() != null) {
      log.debug("Wiki page {} not found or deleted, skipping ingest", pageId);
      return;
    }

    String text = page.getContentText();
    if (text == null || text.isBlank()) {
      log.debug("Wiki page {} has blank content, skipping ingest", pageId);
      return;
    }

    WikiSpace space = wikiSpaceRepository.findById(spaceId).orElse(null);
    Long projectId = space != null ? space.getProjectId() : null;
    String spaceKey = space != null ? space.getSpaceKey() : String.valueOf(spaceId);
    String sourceUrl = "/wiki/" + spaceKey + "/" + pageId;

    List<RawChunk> chunks = splitIntoChunks(text, page.getTitle(), sourceUrl);
    if (chunks.isEmpty()) return;

    knowledgeIngestionService.ingestChunks(
        chunks, KnowledgeEntityType.WIKI_PAGE, pageId, null, projectId);
    log.info(
        "Ingested {} chunks for wiki page {} (space {})", chunks.size(), pageId, spaceId);
  }

  private void handleDelete(Long pageId) {
    knowledgeItemRepository.deleteByEntityTypeAndEntityId(KnowledgeEntityType.WIKI_PAGE, pageId);
    log.info("Removed knowledge items for deleted wiki page {}", pageId);
  }

  private List<RawChunk> splitIntoChunks(String text, String pageTitle, String sourceUrl) {
    List<RawChunk> chunks = new ArrayList<>();
    int ord = 0;
    for (int i = 0; i < text.length(); i += CHUNK_SIZE - CHUNK_OVERLAP) {
      String body = text.substring(i, Math.min(i + CHUNK_SIZE, text.length()));
      chunks.add(
          RawChunk.builder()
              .title(pageTitle + " — part " + (ord + 1))
              .content(body)
              .ordinal(ord++)
              .sourceUrl(sourceUrl)
              .hash(sha256Hex(body))
              .build());
      if (i + CHUNK_SIZE >= text.length()) break;
    }
    return chunks;
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
