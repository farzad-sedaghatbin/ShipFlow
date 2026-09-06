package com.github.farzadsedaghatbin.shipflow.service;

import com.github.farzadsedaghatbin.shipflow.presence.PresenceEntityType;
import com.github.farzadsedaghatbin.shipflow.presence.PresenceService;
import com.github.farzadsedaghatbin.shipflow.presence.PresenceViewerDTO;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Broadcasts wiki-page live-refresh events (v1.13.0 S64), modeled directly on {@link
 * com.github.farzadsedaghatbin.shipflow.service.retro.RetroSseService}.
 *
 * <p>See {@link PitchSseService}'s Javadoc for the rationale shared by both new SSE services: the
 * audience is deliberately "current presence viewers only" (not all space-permitted users), and
 * the broadcast runs synchronously/in-transaction rather than {@code @Async}/{@code
 * REQUIRES_NEW} since the audience comes from Redis, not a DB read.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WikiSseService {

  private static final String EVENT_NAME = "wiki-updated";

  private final NotificationSseManager notificationSseManager;
  private final PresenceService presenceService;

  public void broadcastPageUpdate(Long pageId) {
    List<PresenceViewerDTO> viewers =
        presenceService.getViewers(PresenceEntityType.WIKI_PAGE, pageId);
    for (PresenceViewerDTO viewer : viewers) {
      notificationSseManager.sendEventToUser(
          viewer.getUserId(), EVENT_NAME, Map.of("pageId", pageId));
    }
    log.debug("Broadcast wiki-updated for page {} to {} presence viewer(s)", pageId, viewers.size());
  }
}
