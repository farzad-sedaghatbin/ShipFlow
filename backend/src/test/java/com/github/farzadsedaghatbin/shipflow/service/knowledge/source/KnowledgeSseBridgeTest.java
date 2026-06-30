package com.github.farzadsedaghatbin.shipflow.service.knowledge.source;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.github.farzadsedaghatbin.shipflow.service.NotificationSseManager;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceCreatedEvent;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceDeletedEvent;
import com.github.farzadsedaghatbin.shipflow.service.knowledge.source.event.KnowledgeSourceUpdatedEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KnowledgeSseBridgeTest {

  NotificationSseManager sse = mock(NotificationSseManager.class);
  KnowledgeSseBridge bridge = new KnowledgeSseBridge(sse);

  @Test
  void created_broadcasts_updated() {
    bridge.onCreated(new KnowledgeSourceCreatedEvent(7L));
    verify(sse).broadcast(eq("knowledge.source.updated"), eq(Map.of("sourceId", 7L)));
  }

  @Test
  void updated_broadcasts_updated() {
    bridge.onUpdated(new KnowledgeSourceUpdatedEvent(7L));
    verify(sse).broadcast(eq("knowledge.source.updated"), eq(Map.of("sourceId", 7L)));
  }

  @Test
  void deleted_broadcasts_deleted_with_items() {
    bridge.onDeleted(new KnowledgeSourceDeletedEvent(7L, List.of(1L, 2L)));
    verify(sse)
        .broadcast(
            eq("knowledge.source.deleted"),
            eq(Map.of("sourceId", 7L, "itemIds", List.of(1L, 2L))));
  }
}
