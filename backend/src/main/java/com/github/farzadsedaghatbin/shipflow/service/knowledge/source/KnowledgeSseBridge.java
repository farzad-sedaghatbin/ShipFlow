package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import com.github.farzadsedaghatbin.shipflow.service.NotificationSseManager;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceDeletedEvent;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceUpdatedEvent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridges Knowledge Center domain events to the broadcast SSE channel so any client viewing
 * {@code /knowledge} receives live updates without polling.
 *
 * <p>Knowledge events are app-wide rather than user-targeted, so we use
 * {@link NotificationSseManager#broadcast(String, Object)} instead of per-user delivery.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KnowledgeSseBridge {

  private static final String EVT_UPDATED = "knowledge.source.updated";
  private static final String EVT_DELETED = "knowledge.source.deleted";

  private final NotificationSseManager sse;

  @EventListener
  public void onCreated(KnowledgeSourceCreatedEvent e) {
    sse.broadcast(EVT_UPDATED, Map.of("sourceId", e.sourceId()));
  }

  @EventListener
  public void onUpdated(KnowledgeSourceUpdatedEvent e) {
    sse.broadcast(EVT_UPDATED, Map.of("sourceId", e.sourceId()));
  }

  @EventListener
  public void onDeleted(KnowledgeSourceDeletedEvent e) {
    sse.broadcast(EVT_DELETED, Map.of("sourceId", e.sourceId(), "itemIds", e.knowledgeItemIds()));
  }
}
