package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.github.farzadsedaghatbin.shipflow.entity.enums.KnowledgeSourceStatus;
import com.github.farzadsedaghatbin.shipflow.repository.KnowledgeSourceRepository;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshScheduler {

  private final KnowledgeSourceRepository repo;
  private final KnowledgeSourceRegistry registry;
  private final ApplicationEventPublisher events;
  private final KnowledgeSourceService svc;

  /**
   * Daily 03:30: re-fetch refreshable sources older than 24h. ETag short-circuits unchanged
   * content.
   */
  @Scheduled(cron = "0 30 3 * * *")
  public void refreshDaily() {
    var cutoff = OffsetDateTime.now().minusHours(24);
    int dispatched = 0;
    for (var s : repo.findRefreshCandidates(KnowledgeSourceStatus.READY, cutoff)) {
      if (registry.isAvailable(s.getProviderType())
          && registry.get(s.getProviderType()).supportsRefresh()) {
        events.publishEvent(new KnowledgeSourceCreatedEvent(s.getId()));
        dispatched++;
      }
    }
    if (dispatched > 0) {
      log.info("Knowledge Center refresh: dispatched {} sources", dispatched);
    }
  }

  /** Hourly: flip READY to STALE for sources whose content is older than 30 days. */
  @Scheduled(cron = "0 0 * * * *")
  public void markStaleHourly() {
    var cutoff = OffsetDateTime.now().minusDays(30);
    int marked = 0;
    for (var s : repo.findAll()) {
      if (s.getDeletedAt() == null
          && s.getStatus() == KnowledgeSourceStatus.READY
          && s.getLastIngestedAt() != null
          && s.getLastIngestedAt().isBefore(cutoff)) {
        svc.markStale(s.getId());
        marked++;
      }
    }
    if (marked > 0) {
      log.info("Knowledge Center STALE marker: flipped {} sources", marked);
    }
  }
}
