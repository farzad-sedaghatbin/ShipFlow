package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.CreateKnowledgeSourceRequest;
import com.github.farzadsedaghatbin.shipflow.dto.knowledge.KnowledgeSourceResponse;
import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceScope;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeItemRepository;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceDeletedEvent;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

class KnowledgeSourceServiceTest {

  @Mock KnowledgeSourceRepository sources;
  @Mock KnowledgeItemRepository items;
  @Mock KnowledgeSourceRegistry registry;
  @Mock KnowledgeSourceAccessChecker acl;
  @Mock EmbeddingStore<TextSegment> embeddingStore;
  @Mock ApplicationEventPublisher events;
  @Mock KnowledgeSourceProvider provider;

  private final ObjectMapper json = new ObjectMapper();
  private KnowledgeSourceService svc;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    svc =
        new KnowledgeSourceService(sources, items, registry, acl, embeddingStore, events, json);
  }

  @Test
  void create_validates_config_and_publishes_event() throws Exception {
    CreateKnowledgeSourceRequest req = new CreateKnowledgeSourceRequest();
    req.setName("Engineering Wiki");
    req.setDescription("Internal handbook");
    req.setProviderType(KnowledgeProviderType.URL);
    req.setScope(KnowledgeSourceScope.ORG);
    req.setConfig(json.readTree("{\"url\":\"https://example.com\"}"));

    when(registry.get(KnowledgeProviderType.URL)).thenReturn(provider);
    when(sources.save(any(KnowledgeSource.class)))
        .thenAnswer(
            inv -> {
              KnowledgeSource s = inv.getArgument(0);
              s.setId(7L);
              s.setCreatedAt(OffsetDateTime.now());
              s.setUpdatedAt(OffsetDateTime.now());
              return s;
            });
    when(items.countActiveBySourceId(7L)).thenReturn(0L);

    KnowledgeSourceResponse resp = svc.create(req, 42L);

    verify(provider).validateConfig(any());
    verify(acl).assertCanCreate(KnowledgeSourceScope.ORG, null, null, 42L);

    ArgumentCaptor<KnowledgeSourceCreatedEvent> cap =
        ArgumentCaptor.forClass(KnowledgeSourceCreatedEvent.class);
    verify(events).publishEvent(cap.capture());
    assertThat(cap.getValue().sourceId()).isEqualTo(7L);

    assertThat(resp.getId()).isEqualTo(7L);
    assertThat(resp.getStatus()).isEqualTo(KnowledgeSourceStatus.PENDING);
  }

  @Test
  void delete_cascades_softdelete_items_and_removes_vectors() {
    KnowledgeSource src =
        KnowledgeSource.builder()
            .id(7L)
            .name("S")
            .providerType(KnowledgeProviderType.URL)
            .scope(KnowledgeSourceScope.ORG)
            .config("{}")
            .status(KnowledgeSourceStatus.READY)
            .createdBy(1L)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    when(sources.findActiveById(7L)).thenReturn(Optional.of(src));
    when(items.findIdsBySourceId(7L)).thenReturn(List.of(101L, 102L));
    when(items.softDeleteBySourceId(eq(7L), any())).thenReturn(2);

    svc.delete(7L, 1L);

    verify(acl).assertCanModify(src, 1L);
    verify(items).softDeleteBySourceId(eq(7L), any());
    verify(embeddingStore).removeAll(eq(List.of("101", "102")));

    ArgumentCaptor<KnowledgeSourceDeletedEvent> cap =
        ArgumentCaptor.forClass(KnowledgeSourceDeletedEvent.class);
    verify(events).publishEvent(cap.capture());
    assertThat(cap.getValue().sourceId()).isEqualTo(7L);
    assertThat(cap.getValue().knowledgeItemIds()).containsExactly(101L, 102L);

    assertThat(src.getDeletedAt()).isNotNull();
    verify(sources).save(src);
  }

  @Test
  void delete_with_no_items_skips_vector_store_call() {
    KnowledgeSource src =
        KnowledgeSource.builder()
            .id(8L)
            .name("Empty")
            .providerType(KnowledgeProviderType.URL)
            .scope(KnowledgeSourceScope.ORG)
            .config("{}")
            .status(KnowledgeSourceStatus.PENDING)
            .createdBy(1L)
            .createdAt(OffsetDateTime.now())
            .updatedAt(OffsetDateTime.now())
            .build();

    when(sources.findActiveById(8L)).thenReturn(Optional.of(src));
    when(items.findIdsBySourceId(8L)).thenReturn(List.of());

    svc.delete(8L, 1L);

    verify(items, never()).softDeleteBySourceId(any(), any());
    verify(embeddingStore, never()).removeAll(any(List.class));
    verify(events, times(1)).publishEvent(any(KnowledgeSourceDeletedEvent.class));
    assertThat(src.getDeletedAt()).isNotNull();
  }
}
