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
@DisplayName("PitchSseService")
class PitchSseServiceTest {

  @Mock private NotificationSseManager notificationSseManager;
  @Mock private PresenceService presenceService;

  @InjectMocks private PitchSseService pitchSseService;

  @Test
  @DisplayName("broadcastPitchUpdate sends pitch-updated with the pitchId payload to every presence viewer")
  void broadcastPitchUpdate_SendsToEveryPresenceViewer() {
    when(presenceService.getViewers(PresenceEntityType.PITCH, 10L))
        .thenReturn(List.of(new PresenceViewerDTO(1L, "Alice"), new PresenceViewerDTO(2L, "Bob")));

    pitchSseService.broadcastPitchUpdate(10L);

    verify(notificationSseManager)
        .sendEventToUser(eq(1L), eq("pitch-updated"), eq(Map.of("pitchId", 10L)));
    verify(notificationSseManager)
        .sendEventToUser(eq(2L), eq("pitch-updated"), eq(Map.of("pitchId", 10L)));
  }

  @Test
  @DisplayName("broadcastPitchUpdate is a no-op when there are no presence viewers")
  void broadcastPitchUpdate_NoViewers_DoesNotSend() {
    when(presenceService.getViewers(PresenceEntityType.PITCH, 10L)).thenReturn(List.of());

    pitchSseService.broadcastPitchUpdate(10L);

    verifyNoInteractions(notificationSseManager);
  }
}
