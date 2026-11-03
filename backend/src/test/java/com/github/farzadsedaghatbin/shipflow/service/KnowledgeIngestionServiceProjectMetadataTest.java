package com.github.farzadsedaghatbin.shipflow.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeItem;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.repository.CycleRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EpicRepository;
import com.github.farzadsedaghatbin.shipflow.repository.EvidenceRepository;
import com.github.farzadsedaghatbin.shipflow.repository.InitiativeRepository;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ManualNoteRepository;
import com.github.farzadsedaghatbin.shipflow.repository.MeetingRepository;
import com.github.farzadsedaghatbin.shipflow.repository.PitchRepository;
import com.github.farzadsedaghatbin.shipflow.repository.QAInteractionRepository;
import com.github.farzadsedaghatbin.shipflow.repository.ReleaseRepository;
import com.github.farzadsedaghatbin.shipflow.repository.WorkLogRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.KnowledgeProjectResolver;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.RawChunk;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Chunks must carry the project they belong to so retrieval can be scoped by project. */
@DisplayName("KnowledgeIngestionService — projectId in embedding metadata")
class KnowledgeIngestionServiceProjectMetadataTest {

  private EmbeddingModel embeddingModel;
  private EmbeddingStore<TextSegment> embeddingStore;
  private KnowledgeProjectResolver resolver;
  private KnowledgeIngestionService service;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() {
    embeddingModel = mock(EmbeddingModel.class);
    embeddingStore = mock(EmbeddingStore.class);
    resolver = mock(KnowledgeProjectResolver.class);
    KnowledgeItemRepository items = mock(KnowledgeItemRepository.class);
    when(items.save(any(KnowledgeItem.class)))
        .thenAnswer(
            inv -> {
              KnowledgeItem i = inv.getArgument(0);
              if (i.getId() == null) {
                i.setId(1L);
              }
              return i;
            });
    when(embeddingModel.embed(any(TextSegment.class)))
        .thenReturn(Response.from(Embedding.from(new float[] {1f, 0f})));

    service =
        new KnowledgeIngestionService(
            items, mock(QAInteractionRepository.class), embeddingModel, embeddingStore, null,
            mock(PitchRepository.class), mock(MeetingRepository.class), mock(WorkLogRepository.class),
            mock(CycleRepository.class), mock(EvidenceRepository.class),
            mock(ManualNoteRepository.class), mock(InitiativeRepository.class),
            mock(EpicRepository.class), mock(ReleaseRepository.class));
    service.setProjectResolver(resolver);
  }

  private TextSegment storedSegment() {
    ArgumentCaptor<TextSegment> captor = ArgumentCaptor.forClass(TextSegment.class);
    verify(embeddingStore).add(any(Embedding.class), captor.capture());
    return captor.getValue();
  }

  private static List<RawChunk> oneChunk() {
    return List.of(RawChunk.builder().title("t").content("hello").ordinal(0).hash("h").build());
  }

  @Test
  @DisplayName("An explicit projectId (e.g. a wiki space's) is written to the chunk metadata")
  void explicitProjectId() {
    service.ingestChunks(oneChunk(), KnowledgeEntityType.WIKI_PAGE, 10L, null, 42L);

    assertThat(storedSegment().metadata().getString("projectId")).isEqualTo("42");
  }

  @Test
  @DisplayName("Without one, the project is derived from the owning entity")
  void derivedProjectId() {
    when(resolver.resolve(KnowledgeEntityType.KNOWLEDGE_SOURCE, 10L, null, null))
        .thenReturn(Optional.of(77L));

    service.ingestChunks(oneChunk(), KnowledgeEntityType.KNOWLEDGE_SOURCE, 10L, null, null);

    assertThat(storedSegment().metadata().getString("projectId")).isEqualTo("77");
  }

  @Test
  @DisplayName("A chunk with no determinable project carries no projectId (stays visible org-wide)")
  void noProject() {
    when(resolver.resolve(any(), any(), any(), any())).thenReturn(Optional.empty());

    service.ingestChunks(oneChunk(), KnowledgeEntityType.KNOWLEDGE_SOURCE, 10L, null, null);

    assertThat(storedSegment().metadata().getString("projectId")).isNull();
  }
}
