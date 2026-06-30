package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.RawChunk;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class KnowledgeIngestionServiceIngestChunksTest {

  @Autowired
  KnowledgeIngestionService svc;
  @Autowired
  KnowledgeItemRepository items;
  @Autowired
  KnowledgeSourceRepository sources;

  @Test
  void persists_one_item_per_chunk_tagged_with_source() {
    // Seed a KnowledgeSource so the knowledge_items.knowledge_source_id FK resolves.
    KnowledgeSource source = sources.save(KnowledgeSource.builder().name("test-source")
        .providerType(KnowledgeProviderType.FILE_UPLOAD).scope(KnowledgeSourceScope.ORG)
        .config("{}").status(KnowledgeSourceStatus.PENDING).createdBy(1L).build());
    Long sourceId = source.getId();

    var chunks = List.of(
        RawChunk.builder().title("c1").content("hello world").ordinal(0).hash("h0").build(),
        RawChunk.builder().title("c2").content("second chunk").ordinal(1).hash("h1").build());

    svc.ingestChunks(chunks, KnowledgeEntityType.KNOWLEDGE_SOURCE, sourceId, null, null);

    var saved = items.findAll().stream()
        .filter(k -> k.getEntityType() == KnowledgeEntityType.KNOWLEDGE_SOURCE
            && k.getEntityId().equals(sourceId))
        .toList();
    assertThat(saved).hasSize(2);
    assertThat(saved).extracting("title").containsExactlyInAnyOrder("c1", "c2");
    assertThat(saved)
        .allSatisfy(item -> assertThat(item.getKnowledgeSourceId()).isEqualTo(sourceId));
  }
}
