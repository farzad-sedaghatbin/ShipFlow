package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeEntityType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.service.KnowledgeIngestionService;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class IngestionOrchestratorTest {

  @Mock KnowledgeSourceRepository sources;
  @Mock KnowledgeSourceRegistry registry;
  @Mock KnowledgeIngestionService ingest;
  @Mock KnowledgeSourceService svc;

  private ObjectMapper json;
  private IngestionOrchestrator orch;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    json = new ObjectMapper();
    orch = new IngestionOrchestrator(sources, registry, ingest, svc, json);
  }

  @Test
  void happy_path_marks_ready_and_persists_chunks() {
    var src =
        KnowledgeSource.builder()
            .id(1L)
            .scope(KnowledgeSourceScope.ORG)
            .providerType(KnowledgeProviderType.URL)
            .config("{\"url\":\"http://x\"}")
            .createdBy(99L)
            .build();
    when(sources.findActiveById(1L)).thenReturn(Optional.of(src));
    var provider = mock(KnowledgeSourceProvider.class);
    when(registry.get(KnowledgeProviderType.URL)).thenReturn(provider);
    var result =
        IngestionResult.builder()
            .chunks(
                List.of(RawChunk.builder().title("t").content("c").ordinal(0).hash("h").build()))
            .sourceMetadata(Map.of("etag", "abc"))
            .build();
    when(provider.ingest(eq(src), any())).thenReturn(result);

    orch.onCreated(new KnowledgeSourceCreatedEvent(1L));

    verify(svc).markIngesting(1L);
    verify(ingest)
        .ingestChunks(
            eq(result.getChunks()),
            eq(KnowledgeEntityType.KNOWLEDGE_SOURCE),
            eq(1L),
            eq(null),
            eq(null));
    verify(svc).markReady(eq(1L), any());
  }

  @Test
  void marks_failed_when_provider_throws() {
    var src =
        KnowledgeSource.builder()
            .id(2L)
            .scope(KnowledgeSourceScope.ORG)
            .providerType(KnowledgeProviderType.URL)
            .config("{}")
            .createdBy(99L)
            .build();
    when(sources.findActiveById(2L)).thenReturn(Optional.of(src));
    var provider = mock(KnowledgeSourceProvider.class);
    when(registry.get(KnowledgeProviderType.URL)).thenReturn(provider);
    when(provider.ingest(eq(src), any())).thenThrow(new RuntimeException("boom"));

    orch.onCreated(new KnowledgeSourceCreatedEvent(2L));

    verify(svc).markFailed(eq(2L), contains("boom"));
  }

  @Test
  void skips_when_source_not_found() {
    when(sources.findActiveById(42L)).thenReturn(Optional.empty());

    orch.onCreated(new KnowledgeSourceCreatedEvent(42L));

    verify(svc, org.mockito.Mockito.never()).markIngesting(any());
    verify(svc, org.mockito.Mockito.never()).markReady(any(), any());
    verify(svc, org.mockito.Mockito.never()).markFailed(any(), any());
  }
}
