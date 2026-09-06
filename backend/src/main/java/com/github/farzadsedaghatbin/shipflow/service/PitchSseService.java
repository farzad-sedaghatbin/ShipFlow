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
 * Broadcasts pitch live-refresh events (v1.13.0 S64), modeled directly on {@link
 * com.github.farzadsedaghatbin.shipflow.service.retro.RetroSseService}.
 *
 * <p>Unlike {@code RetroSseService} (which broadcasts to every project member), the audience here
 * is deliberately narrower: only the pitch's current presence viewers, reusing {@link
 * PresenceService#getViewers} directly. This is a considered scope decision for this session —
 * it keeps the change self-contained and avoids a project-membership lookup — not an attempt to
 * retrofit the same narrowing onto retro (which is intentionally left as-is).
 *
 * <p>Called synchronously, in-transaction, straight after {@code PitchService.updatePitch}'s
 * {@code save()} — not {@code @Async}/{@code REQUIRES_NEW} like {@code RetroSseService}. That
 * pattern exists there because {@code broadcastBoardUpdate} does its own repository read
 * ({@code UserProjectRepository.findByProjectId}) to resolve its audience, which only needs to
 * see the caller's own already-in-progress transaction if REQUIRES_NEW is avoided; here the
 * audience comes entirely from Redis via {@link PresenceService}, no DB read is involved, and the
 * SSE emitter writes are fire-and-forget best-effort (a failed send just drops the emitter), so
 * there's nothing that benefits from running outside the caller's transaction/thread. Kept
 * synchronous for simplicity; revisit if this path is ever found to add latency to the update
 * request.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PitchSseService {

  private static final String EVENT_NAME = "pitch-updated";

  private final NotificationSseManager notificationSseManager;
  private final PresenceService presenceService;

  public void broadcastPitchUpdate(Long pitchId) {
    List<PresenceViewerDTO> viewers = presenceService.getViewers(PresenceEntityType.PITCH, pitchId);
    for (PresenceViewerDTO viewer : viewers) {
      notificationSseManager.sendEventToUser(
          viewer.getUserId(), EVENT_NAME, Map.of("pitchId", pitchId));
    }
    log.debug("Broadcast pitch-updated for pitch {} to {} presence viewer(s)", pitchId, viewers.size());
  }
}
