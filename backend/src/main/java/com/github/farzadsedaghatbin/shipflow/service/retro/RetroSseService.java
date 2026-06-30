package com.github.farzadsedaghatbin.shipflow.service.retro;

import com.github.farzadsedaghatbin.shipflow.repository.UserProjectRepository;
import com.github.farzadsedaghatbin.shipflow.service.NotificationSseManager;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Broadcasts retro board change events to all project members via the shared SSE stream.
 * Events are sent asynchronously after the DB transaction commits so emitter writes never
 * participate in the business transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RetroSseService {

  private static final String EVENT_NAME = "retro-updated";

  private final NotificationSseManager sseManager;
  private final UserProjectRepository userProjectRepository;

  @Async
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void broadcastBoardUpdate(Long retroId, Long projectId) {
    if (projectId == null) return;
    Map<String, Object> payload = Map.of("retroId", retroId);
    userProjectRepository.findByProjectId(projectId).forEach(up -> {
      if (up.getUser() != null) {
        sseManager.sendEventToUser(up.getUser().getId(), EVENT_NAME, payload);
      }
    });
    log.debug("Broadcast retro-updated for retro {} to project {} members", retroId, projectId);
  }
}
