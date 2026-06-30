package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.entity.KnowledgeSource;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeProviderType;
import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

class RefreshSchedulerTest {

  KnowledgeSourceRepository repo = mock(KnowledgeSourceRepository.class);
  KnowledgeSourceRegistry reg = mock(KnowledgeSourceRegistry.class);
  ApplicationEventPublisher pub = mock(ApplicationEventPublisher.class);
  KnowledgeSourceService svc = mock(KnowledgeSourceService.class);
  RefreshScheduler s = new RefreshScheduler(repo, reg, pub, svc);

  @Test
  void republishes_ingestion_event_for_refreshable_url_sources() {
    var src =
        KnowledgeSource.builder()
            .id(7L)
            .providerType(KnowledgeProviderType.URL)
            .status(KnowledgeSourceStatus.READY)
            .lastIngestedAt(OffsetDateTime.now().minusDays(2))
            .build();
    when(repo.findRefreshCandidates(eq(KnowledgeSourceStatus.READY), any()))
        .thenReturn(List.of(src));
    when(reg.isAvailable(KnowledgeProviderType.URL)).thenReturn(true);
    var p = mock(KnowledgeSourceProvider.class);
    when(p.supportsRefresh()).thenReturn(true);
    when(reg.get(KnowledgeProviderType.URL)).thenReturn(p);

    s.refreshDaily();

    verify(pub).publishEvent(any(KnowledgeSourceCreatedEvent.class));
  }

  @Test
  void skips_non_refreshable_provider() {
    var src =
        KnowledgeSource.builder()
            .id(8L)
            .providerType(KnowledgeProviderType.FILE_UPLOAD)
            .status(KnowledgeSourceStatus.READY)
            .lastIngestedAt(OffsetDateTime.now().minusDays(2))
            .build();
    when(repo.findRefreshCandidates(eq(KnowledgeSourceStatus.READY), any()))
        .thenReturn(List.of(src));
    when(reg.isAvailable(KnowledgeProviderType.FILE_UPLOAD)).thenReturn(true);
    var p = mock(KnowledgeSourceProvider.class);
    when(p.supportsRefresh()).thenReturn(false);
    when(reg.get(KnowledgeProviderType.FILE_UPLOAD)).thenReturn(p);

    s.refreshDaily();

    verify(pub, never()).publishEvent(any());
  }

  @Test
  void marks_stale_after_30_days() {
    var src =
        KnowledgeSource.builder()
            .id(9L)
            .providerType(KnowledgeProviderType.URL)
            .status(KnowledgeSourceStatus.READY)
            .lastIngestedAt(OffsetDateTime.now().minusDays(31))
            .build();
    when(repo.findAll()).thenReturn(List.of(src));

    s.markStaleHourly();

    verify(svc).markStale(9L);
  }

  @Test
  void does_not_mark_stale_when_fresh() {
    var src =
        KnowledgeSource.builder()
            .id(10L)
            .providerType(KnowledgeProviderType.URL)
            .status(KnowledgeSourceStatus.READY)
            .lastIngestedAt(OffsetDateTime.now().minusDays(5))
            .build();
    when(repo.findAll()).thenReturn(List.of(src));

    s.markStaleHourly();

    verify(svc, never()).markStale(any());
  }
}
