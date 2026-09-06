package com.github.farzadsedaghatbin.shipflow.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.github.farzadsedaghatbin.shipflow.presence.PresenceEntityType;
import com.github.farzadsedaghatbin.shipflow.presence.PresenceService;
import com.github.farzadsedaghatbin.shipflow.presence.PresenceViewerDTO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("WikiSseService")
class WikiSseServiceTest {

  @Mock private NotificationSseManager notificationSseManager;
  @Mock private PresenceService presenceService;

  @InjectMocks private WikiSseService wikiSseService;

  @Test
  @DisplayName("broadcastPageUpdate sends wiki-updated with the pageId payload to every presence viewer")
  void broadcastPageUpdate_SendsToEveryPresenceViewer() {
    when(presenceService.getViewers(PresenceEntityType.WIKI_PAGE, 100L))
        .thenReturn(List.of(new PresenceViewerDTO(1L, "Alice"), new PresenceViewerDTO(2L, "Bob")));

    wikiSseService.broadcastPageUpdate(100L);

    verify(notificationSseManager)
        .sendEventToUser(eq(1L), eq("wiki-updated"), eq(Map.of("pageId", 100L)));
    verify(notificationSseManager)
        .sendEventToUser(eq(2L), eq("wiki-updated"), eq(Map.of("pageId", 100L)));
  }

  @Test
  @DisplayName("broadcastPageUpdate is a no-op when there are no presence viewers")
  void broadcastPageUpdate_NoViewers_DoesNotSend() {
    when(presenceService.getViewers(PresenceEntityType.WIKI_PAGE, 100L)).thenReturn(List.of());

    wikiSseService.broadcastPageUpdate(100L);

    verifyNoInteractions(notificationSseManager);
  }
}
